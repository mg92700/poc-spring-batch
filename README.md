# POC Spring Batch 🚀

Proof of Concept d’une application **Spring Boot + Spring Batch** permettant d’exécuter des traitements batch via une API REST.

Ce projet démontre comment configurer, lancer et superviser des jobs batch dans une application Spring Boot.

---

# 📌 Objectif

Ce projet illustre :

- la configuration d’un **job Spring Batch**
- l’exécution d’un job via une **API REST**
- l’utilisation d’une **base de données pour stocker les métadonnées batch**
- l’intégration avec **Spring Boot**
- la gestion d’un **repository de job**

Spring Batch est un framework permettant de créer des traitements batch robustes et scalables pour les applications Java. :contentReference[oaicite:1]{index=1}

---

# 🏗 Architecture

Architecture simplifiée :

Controller (REST)
│
▼
JobLauncher
│
▼
Spring Batch Job
│
▼
Step
│
▼
Tasklet / Chunk Processing
│
▼
Database (Batch metadata)
