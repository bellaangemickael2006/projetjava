/* ==========================================
   1. INVERSER UNE CHAÎNE
   ========================================== */
function reverseString(str) {
  var result = ""; // on part d'une chaîne vide

  // on lit la chaîne à l'envers, de la dernière lettre à la première
  for (var i = str.length - 1; i >= 0; i--) {
    result = result + str[i]; // on ajoute chaque lettre à la suite
  }

  return result;
}

/* ==========================================
   2. COMPTER LES CARACTÈRES
   ========================================== */
function countCharacters(str) {
  return str.length; // .length donne directement le nombre de caractères
}

/* ==========================================
   3. MAJUSCULE AU DÉBUT DE CHAQUE MOT
   ========================================== */
function capitalizeWords(sentence) {
  var words = sentence.split(" "); // on coupe la phrase en mots
  var result = "";

  for (var i = 0; i < words.length; i++) {
    var word = words[i];
    var premiereLettre = word[0].toUpperCase(); // première lettre en majuscule
    var resteDuMot = word.slice(1); // le reste du mot sans y toucher

    result = result + premiereLettre + resteDuMot + " ";
  }

  return result;
}

/* ==========================================
   4. TROUVER LE MAXIMUM D'UN TABLEAU
   ========================================== */
function findMax(arr) {
  var max = arr[0]; // on suppose que le premier est le plus grand

  for (var i = 0; i < arr.length; i++) {
    if (arr[i] > max) {
      max = arr[i]; // on a trouvé plus grand, on le garde
    }
  }

  return max;
}

/* ==========================================
   5. TROUVER LE MINIMUM D'UN TABLEAU
   ========================================== */
function findMin(arr) {
  var min = arr[0]; // on suppose que le premier est le plus petit

  for (var i = 0; i < arr.length; i++) {
    if (arr[i] < min) {
      min = arr[i]; // on a trouvé plus petit, on le garde
    }
  }

  return min;
}

/* ==========================================
   6. SOMME D'UN TABLEAU
   ========================================== */
function sumArray(arr) {
  var somme = 0;

  for (var i = 0; i < arr.length; i++) {
    somme = somme + arr[i]; // on ajoute chaque nombre
  }

  return somme;
}

/* ==========================================
   7. FILTRER UN TABLEAU (garder les nombres > limite)
   ========================================== */
function filterArray(arr, limite) {
  var result = [];

  for (var i = 0; i < arr.length; i++) {
    if (arr[i] > limite) {
      result.push(arr[i]); // on garde seulement s'il dépasse la limite
    }
  }

  return result;
}

/* ==========================================
   8. FACTORIELLE D'UN NOMBRE
   ========================================== */
function factorial(n) {
  var result = 1;

  for (var i = 1; i <= n; i++) {
    result = result * i; // on multiplie petit à petit
  }

  return result;
}

/* ==========================================
   9. VÉRIFIER SI UN NOMBRE EST PREMIER
   ========================================== */
function isPrime(n) {
  if (n < 2) {
    return false; // 0 et 1 ne sont pas premiers
  }

  for (var i = 2; i < n; i++) {
    if (n % i === 0) {
      return false; // divisible par un autre nombre = pas premier
    }
  }

  return true;
}

/* ==========================================
   10. SUITE DE FIBONACCI
   ========================================== */
function fibonacci(nombreDeTermes) {
  var suite = [0, 1]; // les deux premiers termes sont toujours 0 et 1

  for (var i = 2; i < nombreDeTermes; i++) {
    var terme = suite[i - 1] + suite[i - 2]; // somme des deux précédents
    suite.push(terme);
  }

  return suite;
}

/* ==========================================
   TESTS - regarde le résultat dans la console
   ========================================== */
console.log(reverseString("bonjour"));
console.log(countCharacters("bonjour"));
console.log(capitalizeWords("bonjour tout le monde"));
console.log(findMax([4, 9, 2, 7]));
console.log(findMin([4, 9, 2, 7]));
console.log(sumArray([4, 9, 2, 7]));
console.log(filterArray([1, 5, 8, 2, 10], 4));
console.log(factorial(5));
console.log(isPrime(7));
console.log(fibonacci(8));
