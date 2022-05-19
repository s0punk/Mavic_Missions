# Mavic_Missions
Projet de programmation de drone (DJI Mavic 2 Entreprise)
- Client: Martin Client
- Chef de projet: Simon-Olivier Vaillancourt

# Objectifs du projet

## Suivi d'un parcours dynamique
### Informations de vol
- Vitesse de déplacement: 0.5 m/s
- Hauteur de décollage: 1.2 mètres (par défaut)

### Quatre instructions différente peuvent être détectées
- U: Le drone doit monter d'environ 1 mètre.
- D: Le drone doit descendre d'environ 1 mètre.
- H: Le drone doit attérir et le parcours est terminé.
- Flèche: Le drone doit se déplacer en direction de la flèche.

### Déroulement de l'objectif
1. Le drone va décoller et avancer tout droit sur 1 mètre afin de se placer par-dessus la première pancarte.
2. Le drone va commencer le processus de détection d'instructions. Le drone va effectuer une tentative de détection au 250 ms.
3. Si aucune instruction n'est détectés, le drone va continuer d'avancer, à coup de 2.5 mètres. Après 25 tentatives consécutive d'échouées,
le drone s'arrête et termine le parcours prématurément.
4. Si le drone détecte une instruction, il va s'arrêter brièvement, exécuter l'instruction et va continuer à avancher tout droit. Ensuite, le drone retourne à l'étape 3.


## Suivi d'une ligne verte
### Informations de vol
- Vitesse de déplacement: 0.25 m/s
- Hauteur de décollage: 1.2 mètres (par défaut)

### Intersections
Le drone va se diriger vers l'endroit où il est le plus sûr que la ligne se trouve. Cela dépend du résultat de la détection de la ligne.
Lors des tests, le drone a toujours été tout droit. Il reste possible que le drone décide de tourner.

### Déroulement de l'objectif
1. Le drone va décoller et positionner son gimbale à -80 degrées.
2. Le drone va commencer son processus d'alignement avec la ligne.
2.1 Le drone va détecter un point sur la ligne et calculer l'angle nécessaire pour être droit sur la ligne
2.2 Le drone vérifier si la ligne est trop à droite ou trop à gauche, il se déplace en conséquance.
3. Le drone commence à suivre la ligne, il détecte des points sur la ligne afin de trouver la direction à suivre.
4. Après avoir suivi la ligne pendant environ 1 sec., le drone revérifie son alignement avec la ligne et retourne à l'étape 2.


## Sauvetage d'une balle
### Informations de vol
- Vitesse de déplacement: 1 m/s (vitesse maximum permise)
- Hauteur de décollage: 3.2 mètres

### Déroulement de l'objectif:
1. Le drone décolle et monte de 2 mètres.
2. Le drone commence à chercher la balle.
2.1 Le drone zoom sa caméra au maximum et cherche la balle.
2.2 Si le drone ne voit pas la balle, il dé-zoom sa caméra, repositionne son gimbale et cherche la balle.
2.3 Lorsque le drone arrive au zoom minimum, il rotationne positivement de 25 degrées et retourne à l'étape 2.
3. Lorsque le drone voit la balle, il calcule l'angle nécessaire pour s'aligner avec celle-ci.
4. Le drone avance jusqu'à ce qu'il ne voit plus la balle.
5. Lorsque le drone ne voit plus la balle, il dé-zoom sa caméra, réajuste son gimbale et retourne à l'étape 4.
6. Lorsque le drone arrive au zoom 2, il attérie et le sauvetage est terminé.
7. Si le drone a rotationné sur 360 degrées sans jamais voir la balle, il attérie et termine la recherche.
