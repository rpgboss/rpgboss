// Casting to support both Javascript and Java strings.
function leftPad(string, totalLen) {
  var castedString = String(string);
  return Array(totalLen - castedString.length).join(" ") + castedString;
}

function rightPad(string, totalLen) {
  var castedString = String(string);
  return castedString + Array(totalLen - castedString.length).join(" ");
}