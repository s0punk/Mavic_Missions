# Mavic_Missions
Projet de programmation de drone (DJI Mavic 2 Entreprise)

# Objectifs

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
