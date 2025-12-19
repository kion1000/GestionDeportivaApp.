# Gestión Deportiva App (Android)

App Android para gestionar el club **Atlético Borgoñón**: jugadores, encuentros, estadísticas y cuotas, todo desde el móvil.  
Hecha en **Java** con persistencia en **SQLite** (sin depender de internet). Porque a veces el Wi-Fi también juega de lateral… y la lía.

---

## ✨ Funcionalidades principales

- **Jugadores**
  - Alta / edición / baja de jugadores
  - Datos: nombre, dorsal, posición, etc.
  - **Foto** del jugador (carga/visualización) *(si aplica: Picasso)*

- **Encuentros (Partidos)**
  - Crear y consultar partidos
  - Detalle del encuentro: eventos como **goles, asistencias, tarjetas**, etc.
  - Listados con **RecyclerView** + adapters

- **Estadísticas**
  - Resumen por jugador y/o por partido (goles, tarjetas, asistencias…)
  - Listas optimizadas y sin duplicados (uso de `Set/Map` cuando hace falta)

- **Cuotas**
  - Control de cuotas (ej: seguro, cuota 1, cuota 2…)
  - Estado de pago por jugador *(si aplica)*

---

## 🧱 Tecnologías

- **Android Studio**
- **Java**
- **SQLite** (persistencia local)
- UI: **RecyclerView**, **Spinners**, Activities
- *(Opcional si lo usas)* **Picasso** para imágenes
- Arquitectura simple por capas (UI → Data/DBHelper → Model)

---

## 🗂️ Estructura del proyecto (orientativa)

app/
├─ ui/ # Activities, adapters, pantallas
├─ data/ # DBHelper, queries, repositorios (si existen)
├─ model/ # Clases POJO: Jugador, Encuentro, Evento...
└─ res/ # layouts, drawables, strings, etc.


---

## 🛢️ Base de datos (SQLite)

La app guarda los datos en SQLite mediante `DBHelper`.

Tablas típicas (según tu implementación):
- `jugadores`
- `encuentros`
- `eventos` (goles/tarjetas/asistencias)
- `cuotas` / `pagos` *(si aplica)*

---

## ▶️ Cómo ejecutar

1. Abre el proyecto en **Android Studio**
2. Sync Gradle
3. Ejecuta en:
   - Emulador o dispositivo físico
4. Listo.

Requisitos recomendados:
- Android Studio actualizado
- SDK instalado (minSdk/targetSdk según tu `build.gradle`)
- Dispositivo con Android

---

## 🧭 Roadmap (próximas mejoras)

- [ ] Filtros y búsqueda de jugadores/encuentros
- [ ] Exportar estadísticas a PDF/CSV
- [ ] Pantalla “ranking” (goles, asistencias, tarjetas)
- [ ] Copias de seguridad (export/import de la BD)
- [ ] Mejoras UX (snackbars, deshacer borrar, confirmaciones)

---

## 🐛 Problemas conocidos

- (Ejemplo) “Duplicados en eventos si se registran muy rápido”

---

## 📸 Capturas

<img width="481" height="767" alt="image" src="https://github.com/user-attachments/assets/1128d3b6-e759-4385-9f20-0381f24097dc" />
<img width="477" height="753" alt="image" src="https://github.com/user-attachments/assets/7a58056d-5396-4d04-96e1-470429782a0d" />
<img width="477" height="741" alt="image" src="https://github.com/user-attachments/assets/dee7146a-8344-4c0d-b533-92893efec50a" />
<img width="478" height="733" alt="image" src="https://github.com/user-attachments/assets/3d7770d5-85e2-46b5-9cd9-76f1a5f8fd14" />


---

## 👤 Autor

- **Jonay Armas Suárez**
- Proyecto: DAM / App móvil de gestión para club deportivo

---

## 📄 Licencia

Uso personal/educativo.  


