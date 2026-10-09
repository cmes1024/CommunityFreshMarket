function memberCodeEntry(store) {
  return store && String(store.memberCodeEntry).toUpperCase() === "TAB" ? "TAB" : "PROFILE";
}

module.exports = { memberCodeEntry };
