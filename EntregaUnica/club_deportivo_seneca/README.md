## Requisitos
Java 17 o superior.
Eclipse IDE con JUnit 5.

## Importar el proyecto
1. Abra Eclipse.
2. Seleccione File > Import.
3. Seleccione General > Existing Projects into Workspace.
4. Elija la carpeta de este proyecto (la que contiene `src`, `test` y `datos`).
5. Seleccione club_deportivo_seneca y pulse Finish.

## Ejecutar las consolas
Hay un programa (metodo `main`) por cada tipo de usuario, todos en `src/cds/consola`:

| Usuario | Clase | Usuario de prueba |
|---|---|---|
| Administrador | `ConsolaAdministrador` | admin / admin123 |
| Socio | `ConsolaSocio` | ana / ana123, carlos / carlos123 |
| Empleado (entrenador o fisioterapeuta) | `ConsolaEmpleado` | leo / leo123 (entrenador), paula / paula123 (fisioterapeuta) |

1. Abra la clase y seleccione Run As > Java Application.
2. El directorio de trabajo debe ser la carpeta del proyecto (es lo predeterminado en Eclipse),
   para que se encuentre `datos/club.json`.
3. Inicie sesion con su login y password.
4. Al elegir `0` en el menu principal, los cambios se guardan en el mismo `datos/club.json`.

## Estructura de la consola
- `ConsolaBase`: carga los datos, autentica, muestra el menu del rol y guarda al salir.
- `Entrada`: lee y valida todas las entradas (numeros, fechas, horas, s/n, listas).
- `Menu` y `Accion`: menu numerado reutilizable; para agregar una opcion basta con `menu.agregar("texto", accion)`.
- Un menu por area: `MenuAdminPersonal`, `MenuAdminDeportes`, `MenuAdminTienda`, `MenuAdminTorneos`, `MenuEntrenador`,
  `MenuFisioterapeuta`, `MenuCompras`, `MenuTorneosJugador` (socios y empleados) y `RegistroPartido`.
- `VistaTorneos`: impresion de listas, tablas de posiciones e historial de torneos.

## Pruebas
Las pruebas JUnit 5 estan en la carpeta `test`.
`test/cds/historias` tiene una clase por historia de usuario del banco oficial; algunas tienen clases `@Nested`
que prueban cada criterio de aceptacion.

## Datos de prueba
`datos/club.json` trae datos listos para probar cada historia del banco oficial desde la consola.
Todos los usuarios tienen password = login + `123` (ej: `julia` / `julia123`).

| HU | Consola y usuario | Como probarla |
|---|---|---|
| HU-ADM-01 | Administrador (`admin`) | Tienda > Catalogo > Trasladar: Raqueta de Bodega a Tienda (funciona); repetirlo (rechaza: ya no esta en origen); Balon de Bodega a Tienda (rechaza: ya esta en destino) |
| HU-ADM-02 | Administrador (`admin`) | Usuarios y turnos > Resolver solicitudes: la de Maximo se aprueba; la de Sara (domingo) se rechaza por falta de cobertura |
| HU-ADM-03 | Administrador (`admin`) | Tienda > Revisar sugerencias: "Barra de proteina" se aprueba y entra al catalogo; "Galletas de avena" no se deja aprobar (sin alergenos). El empleado ve el estado en "Ver el estado de mis sugerencias" |
| HU-ENT-01 | Empleado (`leo`) | Partidos > Registrar partido oficial en la Liga Universitaria: incluir a Laura (rechaza: no esta en la nomina), a Julia (rechaza: suspendida por roja) o a otros 7 (se registra) |
| HU-ENT-02 | Empleado (`leo`) | Partidos > Verificar minimo: Seneca Rojo si puede jugar (Julia no cuenta); Seneca Basket no puede (4 habilitados, Gabriel con restriccion medica) |
| HU-JUG-01 | Socio (`carlos`) | Academias > Reservar en Academia de Tenis el 2026-12-01 de 10:00 a 11:00 (rechaza: 2 instalaciones ya ocupadas). Tienda y cafeteria > comprar un Latte (recibirlo en Cancha Tenis 1) y luego reservar la Cancha Padel 1 (rechaza por bebida caliente) |
| HU-SOC-01 | Socio (`carlos`) | Academias > Reservar en Academia de Padel con 2 jugadores (rechaza: el padel es de dobles); con DOBLES y 4 jugadores (funciona) |
| HU-SOC-02 | Socio (`hugo`) | Inscribirme en un equipo: Seneca Sub-15 (rechaza por categoria); Seneca Azul con un dorsal libre (funciona). Con `ana`: Seneca Azul (rechaza: ya juega futbol) |

### Torneos de demostracion
| Torneo | Estado | Para probar |
|---|---|---|
| Copa Recreativa Tenis de Mesa | Inscripciones abiertas | Socio > Torneos > Inscribirme. Los 4 cupos regulares estan llenos y queda 1 preferencial: `laura` no puede entrar (cupo reservado al ranking); `felipe` (primero del ranking) si entra como preferencial |
| Abierto Competitivo de Tenis | Inscripciones abiertas | Tarifa de $50.000; quedan 7 cupos. Probar inscribirse y salirse como socio (`laura`) o empleado (`max`) |
| Copa Competitiva de Futbol | Inscripciones abiertas | Torneo por equipos con Seneca Rojo inscrito (preferencial por su desempeno) |
| Liga Recreativa de Tenis | En curso | Registrar los 2 resultados pendientes y finalizarla (Administrador > Torneos) |
| Torneo Recreativo de Padel Dobles | Finalizado | Ver la tabla de posiciones y el historial; ganaron Carlos y Diana |

Si quiere volver a los datos originales despues de probar: `git restore datos/club.json`
