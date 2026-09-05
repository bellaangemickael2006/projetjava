document.addEventListener("DOMContentLoaded", function () {

  var colorBox = document.getElementById("color-box");
  var changeColorBtn = document.getElementById("change-color-btn");

  function getRandomColor() {
    var letters = "0123456789ABCDEF";
    var color = "#";

    for (var i = 0; i < 6; i++) {
      var randomIndex = Math.floor(Math.random() * letters.length);
      color = color + letters[randomIndex];
    }

    return color;
  }

  changeColorBtn.addEventListener("click", function () {
    colorBox.style.backgroundColor = getRandomColor();
  });

});
