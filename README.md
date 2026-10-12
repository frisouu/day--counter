# Day Counter — Fabric 26.3

Affiche « Jour N » en haut à gauche : Montserrat Black, contour bleu ciel, ombre portée. Mod client uniquement.

## Compiler
Prérequis : JDK 25.
```
./gradlew build        (Windows : gradlew.bat build)
```
Le jar est dans `build/libs/day-counter-1.0.0.jar` → à mettre dans `.minecraft/mods/` avec Fabric Loader 0.19.5+ et Fabric API 26.3.

## Réglages
Constantes en haut de `DayCounterClient.java` (position, couleurs ARGB, épaisseur contour, décalage ombre, libellé).
Taille de police : `size` dans `assets/daycounter/font/montserrat.json`.

## Licence police
Montserrat — SIL Open Font License 1.1 (voir `OFL.txt`), redistribution autorisée dans le mod.
