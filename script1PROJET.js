document.addEventListener("DOMContentLoaded", function () {

  var totalPriceElement = document.querySelector(".total");

  // Recalcule et affiche le prix total en fonction de tous les produits restants
  function updateTotalPrice() {
    var total = 0;
    var products = document.querySelectorAll(".list-products > .card-body");

    products.forEach(function (product) {
      var unitPriceText = product.querySelector(".unit-price").textContent;
      var unitPrice = parseFloat(unitPriceText); // "100 $" -> 100

      var quantityText = product.querySelector(".quantity").textContent;
      var quantity = parseInt(quantityText);

      total += unitPrice * quantity;
    });

    totalPriceElement.textContent = total + " $";
  }

  // Branche les événements (+ / - / poubelle / coeur) sur un produit donné
  function setupProduct(product) {
    var quantityElement = product.querySelector(".quantity");
    var plusButton = product.querySelector(".fa-plus-circle");
    var minusButton = product.querySelector(".fa-minus-circle");
    var trashButton = product.querySelector(".fa-trash-alt");
    var heartButton = product.querySelector(".fa-heart");

    // Bouton "+" : augmente la quantité
    plusButton.addEventListener("click", function () {
      var quantity = parseInt(quantityElement.textContent);
      quantity = quantity + 1;
      quantityElement.textContent = quantity;
      updateTotalPrice();
    });

    // Bouton "-" : diminue la quantité (jamais en dessous de 0)
    minusButton.addEventListener("click", function () {
      var quantity = parseInt(quantityElement.textContent);
      if (quantity > 0) {
        quantity = quantity - 1;
        quantityElement.textContent = quantity;
        updateTotalPrice();
      }
    });

    // Bouton poubelle : supprime le produit du panier
    trashButton.addEventListener("click", function () {
      product.remove();
      updateTotalPrice();
    });

    // Bouton coeur : bascule l'état "aimé" et change la couleur
    heartButton.addEventListener("click", function () {
      heartButton.classList.toggle("liked");
    });
  }

  // On applique les événements à chaque produit déjà présent au chargement
  var products = document.querySelectorAll(".list-products > .card-body");
  products.forEach(function (product) {
    setupProduct(product);
  });

  // Prix total initial (0 $ puisque toutes les quantités démarrent à 0)
  updateTotalPrice();

});
