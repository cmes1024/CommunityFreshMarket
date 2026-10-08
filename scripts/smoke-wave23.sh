#!/usr/bin/env bash
# Wave 2/3 API smoke scenarios. Do not require interactive approval.
# Usage: bash scripts/smoke-wave23.sh
set -euo pipefail
BASE="${BASE:-http://127.0.0.1:8088}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

eval "$(python3 - <<PY
import hmac, hashlib, json, base64, time
secret = [l.split('=',1)[1].strip() for l in open('$ROOT/deploy/.env') if l.startswith('JWT_SECRET=')][0]

def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode()

def issue(uid: int) -> str:
    header = b64url(json.dumps({"alg":"HS256","typ":"JWT"}, separators=(',',':')).encode())
    now = int(time.time())
    payload = b64url(json.dumps({"sub":str(uid),"iat":now,"exp":now+86400}, separators=(',',':')).encode())
    sig = hmac.new(secret.encode(), f"{header}.{payload}".encode(), hashlib.sha256).digest()
    return f"{header}.{payload}.{b64url(sig)}"

print(f"OWNER_TOKEN={issue(6)}")
print(f"CUST_TOKEN={issue(2)}")
print(f"CUST2_TOKEN={issue(3)}")
print(f"STAFF_TOKEN={issue(1)}")
PY
)"

pass=0
fail=0
assert_json() {
  local name="$1" want="$2" body="$3"
  if echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); sys.exit(0 if ($want) else 1)"; then
    echo "OK  $name"
    pass=$((pass+1))
  else
    echo "FAIL $name"
    echo "  body=$body"
    fail=$((fail+1))
  fi
}

req() {
  local method="$1" path="$2" token="$3" data="${4:-}"
  if [[ -n "$data" ]]; then
    curl -sS -X "$method" "$BASE$path" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json" \
      -d "$data"
  else
    curl -sS -X "$method" "$BASE$path" \
      -H "Authorization: Bearer $token"
  fi
}

echo "=== health ==="
H=$(curl -sS "$BASE/api/v1/health")
assert_json "health" "d.get('code')==0 and d['data']['status']=='UP'" "$H"

echo "=== auth me ==="
ME=$(req GET /api/v1/me "$OWNER_TOKEN")
assert_json "owner me" "d.get('code')==0 and d['data']['id']==6" "$ME"
ME2=$(req GET /api/v1/me "$CUST_TOKEN")
assert_json "cust me" "d.get('code')==0 and d['data']['id']==2" "$ME2"

echo "=== product qty limits ==="
# create product with stock=3, min=2, max=2
CREATE=$(req POST /api/v1/merchant/products "$OWNER_TOKEN" \
  '{"categoryId":1,"name":"SMOKE起购蛋","unit":"PORTION","priceFen":500,"inStock":true,"onShelf":true,"stockQty":3,"minQty":2,"maxQty":2}')
assert_json "create limited product" "d.get('code')==0" "$CREATE"
PID=$(echo "$CREATE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# cart add qty=1 should fail (below min)
ADD1=$(req POST /api/v1/cart/items "$CUST_TOKEN" "{\"productId\":$PID,\"qty\":1}")
assert_json "reject below min" "d.get('code')!=0" "$ADD1"

# cart add qty=2 ok
ADD2=$(req POST /api/v1/cart/items "$CUST_TOKEN" "{\"productId\":$PID,\"qty\":2}")
assert_json "add min qty" "d.get('code')==0" "$ADD2"

# set qty=3 should fail (over max and stock)
SET3=$(req PUT "/api/v1/cart/items/$PID" "$CUST_TOKEN" '{"qty":3}')
assert_json "reject over max" "d.get('code')!=0" "$SET3"

echo "=== order deduct stock ==="
# need address for delivery or pickup slot
ADDR=$(req POST /api/v1/addresses "$CUST_TOKEN" \
  '{"phone":"13800000000","community":"SMOKE小区","building":"1","room":"101","isDefault":true}')
assert_json "create address" "d.get('code')==0" "$ADDR"
AID=$(echo "$ADDR" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

# clear other cart noise: set only our product by re-set qty 2
req PUT "/api/v1/cart/items/$PID" "$CUST_TOKEN" '{"qty":2}' >/dev/null
# remove other products if any - get cart and remove
CART=$(req GET /api/v1/cart "$CUST_TOKEN")
echo "$CART" | python3 -c "
import sys,json,urllib.request,os
cart=json.load(sys.stdin)['data']
token=os.environ.get('T','')
" 2>/dev/null || true

# remove other items
for oid in $(echo "$CART" | python3 -c "import sys,json; print(' '.join(str(i['productId']) for i in json.load(sys.stdin)['data']['items'] if i['productId']!=$PID))"); do
  req DELETE "/api/v1/cart/items/$oid" "$CUST_TOKEN" >/dev/null || true
done
req PUT "/api/v1/cart/items/$PID" "$CUST_TOKEN" '{"qty":2}' >/dev/null

ORDER=$(req POST /api/v1/orders "$CUST_TOKEN" \
  "{\"fulfillment\":\"DELIVERY\",\"addressId\":$AID,\"asap\":true,\"remark\":\"smoke\"}")
assert_json "submit order" "d.get('code')==0" "$ORDER"
OID=$(echo "$ORDER" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

PROD=$(req GET "/api/v1/merchant/products/$PID" "$OWNER_TOKEN")
assert_json "stock deducted to 1" "d.get('code')==0 and float(d['data']['stockQty'])==1" "$PROD"

echo "=== cancel restores stock ==="
CAN=$(req POST "/api/v1/orders/$OID/cancel" "$CUST_TOKEN" '{"reason":"smoke cancel"}')
assert_json "cancel order" "d.get('code')==0" "$CAN"
PROD2=$(req GET "/api/v1/merchant/products/$PID" "$OWNER_TOKEN")
assert_json "stock restored to 3" "d.get('code')==0 and float(d['data']['stockQty'])==3" "$PROD2"

echo "=== order complete awards points ==="
# re-order for points
req PUT "/api/v1/cart/items/$PID" "$CUST_TOKEN" '{"qty":2}' >/dev/null
ORDER2=$(req POST /api/v1/orders "$CUST_TOKEN" \
  "{\"fulfillment\":\"DELIVERY\",\"addressId\":$AID,\"asap\":true}")
assert_json "submit order2" "d.get('code')==0" "$ORDER2"
OID2=$(echo "$ORDER2" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
# advance PENDING_CONFIRM -> PREPARING -> DELIVERING -> COMPLETED
for st in PENDING_CONFIRM PREPARING DELIVERING; do
  ADV=$(req POST "/api/v1/merchant/orders/$OID2/advance" "$OWNER_TOKEN" "{\"expectedStatus\":\"$st\"}")
  assert_json "advance $st" "d.get('code')==0" "$ADV"
done
PTS=$(req GET /api/v1/points/me "$CUST_TOKEN")
# totalFen = 500*2 = 1000 fen = 10 yuan * ratio 1 = 10 points
assert_json "points earned >=10" "d.get('code')==0 and d['data']['balance']>=10" "$PTS"

echo "=== redeem ==="
RED=$(req POST /api/v1/points/redeem "$CUST_TOKEN" '{"title":"SMOKE换鸡蛋","costPoints":5}')
assert_json "redeem" "d.get('code')==0 and d['data']['status']=='PENDING'" "$RED"
RID=$(echo "$RED" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
DONE=$(req POST "/api/v1/merchant/points/redeems/$RID/complete" "$OWNER_TOKEN")
assert_json "redeem complete" "d.get('code')==0 and d['data']['status']=='DONE'" "$DONE"

echo "=== live photo ==="
put_live() {
  local enabled="$1" url="$2"
  local body
  body=$(req GET /api/v1/merchant/products/1 "$OWNER_TOKEN" | ENABLED="$enabled" URL="$url" python3 -c '
import json, os, sys
p = json.load(sys.stdin)["data"]
keys = ["categoryId","name","unit","priceFen","inStock","onShelf","homeRecommended","specNote","stockQty","minQty","maxQty","imageUrls","imageUrl"]
out = {k: p.get(k) for k in keys}
out["livePhotoEnabled"] = os.environ["ENABLED"] == "true"
out["livePhotoUrl"] = os.environ["URL"]
print(json.dumps(out, ensure_ascii=False))
')
  req PUT /api/v1/merchant/products/1 "$OWNER_TOKEN" "$body"
}
put_live true "https://example.com/smoke-reset.jpg" >/dev/null
put_live false "" >/dev/null
OFF=$(req POST /api/v1/products/1/live-photo-requests "$CUST_TOKEN")
assert_json "live photo off rejected" "d.get('code')!=0" "$OFF"
EN=$(put_live true "")
assert_json "enable live photo without image" "d.get('code')==0 and d['data']['livePhotoEnabled'] is True" "$EN"
R1=$(req POST /api/v1/products/1/live-photo-requests "$CUST_TOKEN")
assert_json "first request" "d.get('code')==0 and d['data']['applicantCount']==1" "$R1"
R2=$(req POST /api/v1/products/1/live-photo-requests "$CUST2_TOKEN")
assert_json "second customer counts" "d.get('code')==0 and d['data']['applicantCount']==2" "$R2"
R3=$(req POST /api/v1/products/1/live-photo-requests "$CUST_TOKEN")
assert_json "same customer does not increment" "d.get('code')==0 and d['data']['applicantCount']==2" "$R3"
INBOX=$(req GET /api/v1/merchant/live-photo-requests "$OWNER_TOKEN")
assert_json "inbox merged" "d.get('code')==0 and any(x['productId']==1 and x['applicantCount']==2 for x in d['data'])" "$INBOX"
UP=$(put_live true "https://example.com/smoke-live.jpg")
assert_json "submit live photo clears request" "d.get('code')==0 and d['data']['livePhotoUrl']=='https://example.com/smoke-live.jpg'" "$UP"
INBOX2=$(req GET /api/v1/merchant/live-photo-requests "$OWNER_TOKEN")
assert_json "inbox cleared" "d.get('code')==0 and not any(x['productId']==1 for x in d['data'])" "$INBOX2"
FRESH=$(req POST /api/v1/products/1/live-photo-requests "$CUST_TOKEN")
assert_json "fresh photo rejects request" "d.get('code')!=0" "$FRESH"
put_live true "" >/dev/null
req POST /api/v1/products/1/live-photo-requests "$CUST_TOKEN" >/dev/null
OFF2=$(put_live false "")
assert_json "turn off keeps photo url empty" "d.get('code')==0 and d['data']['livePhotoEnabled'] is False" "$OFF2"
INBOX3=$(req GET /api/v1/merchant/live-photo-requests "$OWNER_TOKEN")
assert_json "turn off keeps inbox" "d.get('code')==0 and any(x['productId']==1 for x in d['data'])" "$INBOX3"
BLOCK=$(req POST /api/v1/products/1/live-photo-requests "$CUST2_TOKEN")
assert_json "disabled rejects new request" "d.get('code')!=0" "$BLOCK"
put_live false "" >/dev/null

echo "=== coupon ==="
COUP=$(req POST /api/v1/merchant/coupons "$OWNER_TOKEN" \
  '{"title":"SMOKE减3元","type":"FIXED","faceFen":300,"minSpendFen":1000,"perUserLimit":1,"enabled":true}')
assert_json "create coupon" "d.get('code')==0" "$COUP"
CID=$(echo "$COUP" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
CL=$(req POST "/api/v1/coupons/$CID/claim" "$CUST_TOKEN")
assert_json "claim coupon" "d.get('code')==0 and d['data']['status']=='UNUSED'" "$CL"
CL2=$(req POST "/api/v1/coupons/$CID/claim" "$CUST_TOKEN")
assert_json "claim twice rejected" "d.get('code')!=0" "$CL2"

echo "=== checkin + cashier ==="
CHK=$(req POST /api/v1/checkins "$CUST_TOKEN" '{"lat":31.2,"lng":121.5}')
assert_json "checkin" "d.get('code')==0" "$CHK"
CHKID=$(echo "$CHK" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
ACT=$(req GET /api/v1/merchant/checkins/active "$OWNER_TOKEN")
assert_json "active checkins" "d.get('code')==0 and len(d['data'])>=1" "$ACT"
SALE=$(req POST /api/v1/merchant/cashier "$OWNER_TOKEN")
assert_json "open cashier" "d.get('code')==0 and d['data']['status']=='OPEN'" "$SALE"
SID=$(echo "$SALE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
ITEM=$(req POST "/api/v1/merchant/cashier/$SID/items" "$OWNER_TOKEN" \
  '{"productId":1,"qty":1}')
assert_json "cashier add item" "d.get('code')==0 and d['data']['totalFen']>0" "$ITEM"
MEM=$(req POST "/api/v1/merchant/cashier/$SID/member" "$OWNER_TOKEN" \
  "{\"checkinId\":$CHKID}")
assert_json "attach checkin member" "d.get('code')==0 and d['data']['memberUserId']==2" "$MEM"
CMP=$(req POST "/api/v1/merchant/cashier/$SID/complete" "$OWNER_TOKEN" '{}')
assert_json "cashier complete" "d.get('code')==0 and d['data']['status']=='COMPLETED'" "$CMP"

echo "=== store settings wave2 fields ==="
ST=$(req PUT /api/v1/merchant/store "$OWNER_TOKEN" \
  '{"pointsPerYuan":2,"pickImageTtlMinutes":20}')
assert_json "store points/ttl" "d.get('code')==0 and d['data']['pointsPerYuan']==2 and d['data']['pickImageTtlMinutes']==20" "$ST"
# restore
req PUT /api/v1/merchant/store "$OWNER_TOKEN" '{"pointsPerYuan":1,"pickImageTtlMinutes":15}' >/dev/null

echo "=== edge: stock=0 auto sold-out ==="
SO=$(req POST /api/v1/merchant/products "$OWNER_TOKEN" \
  '{"categoryId":1,"name":"SMOKE自动售罄","unit":"PIECE","priceFen":100,"inStock":true,"onShelf":true,"stockQty":1,"minQty":1,"maxQty":1}')
assert_json "create stock1 product" "d.get('code')==0" "$SO"
SPID=$(echo "$SO" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
CART_CLR=$(req GET /api/v1/cart "$CUST_TOKEN")
for oid in $(echo "$CART_CLR" | python3 -c "import sys,json; print(' '.join(str(i['productId']) for i in json.load(sys.stdin)['data']['items']))"); do
  req DELETE "/api/v1/cart/items/$oid" "$CUST_TOKEN" >/dev/null || true
done
req PUT "/api/v1/cart/items/$SPID" "$CUST_TOKEN" '{"qty":1}' >/dev/null
SO_ORD=$(req POST /api/v1/orders "$CUST_TOKEN" \
  "{\"fulfillment\":\"DELIVERY\",\"addressId\":$AID,\"asap\":true}")
assert_json "order last unit" "d.get('code')==0" "$SO_ORD"
SO_P=$(req GET "/api/v1/merchant/products/$SPID" "$OWNER_TOKEN")
assert_json "auto sold out" "d.get('code')==0 and float(d['data']['stockQty'])==0 and d['data']['inStock'] is False" "$SO_P"

echo "=== edge: live photo ttl ==="
BADTTL=$(req PUT /api/v1/merchant/store "$OWNER_TOKEN" '{"pickImageTtlMinutes":0}')
assert_json "ttl at least 1 minute" "d.get('code')!=0" "$BADTTL"

echo "=== edge: staff merchant access ==="
STAFF_ME=$(req GET /api/v1/merchant/orders "$STAFF_TOKEN")
assert_json "staff can board" "d.get('code')==0" "$STAFF_ME"

echo
echo "RESULT pass=$pass fail=$fail"
[[ "$fail" -eq 0 ]]
