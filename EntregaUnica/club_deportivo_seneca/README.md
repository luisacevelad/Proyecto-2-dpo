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
| Socio, estudiante o externo | `ConsolaSocio` | ana / ana123, carlos / carlos123; estudiante sebas / sebas123; externo pablo / pablo123 |
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
Las pruebas JUnit 5 estan en la carpeta `test`. Para correrlas todas: clic derecho en `test` > Run As > JUnit Test.

| Paquete | Que prueba |
|---|---|
| `test/cds/historias` | Una clase por historia de usuario: las 8 del banco oficial y las 7 de torneos (HU-ADM-04 a 06, HU-EMP-01, HU-SOC-03 a 05), con un `@Nested` por criterio de aceptacion |
| `test/cds/requerimientos` | Requerimientos funcionales del Proyecto 1: ventas (descuentos, IVA, impoconsumo, propina, puntos, bebidas calientes), mensualidades y reporte de ingresos, entrenamientos y restricciones medicas, partidos internos, ligas y ranking, y reglas de reservas, turnos y codigos |
| `test/cds/persistencia` | Guardar y cargar `club.json` sin perder informacion (incluidos los torneos), archivos antiguos y archivos con errores |

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
| Abierto Competitivo de Tenis | Inscripciones abiertas | Empieza un miercoles, tarifa de $50.000 y quedan 5 cupos. Socio (`laura`): inscribirse paga $50.000 y puede salirse. Empleado `max` (sin turno los miercoles): se inscribe sin pagar. Empleado `leo` (turno todos los dias): se rechaza por turno |
| Copa Competitiva de Futbol | Inscripciones abiertas | Torneo por equipos con Seneca Rojo y Seneca Azul inscritos (tarifa de $200.000 por equipo) |
| Liga Recreativa de Tenis | En curso | Registrar los 2 resultados pendientes y finalizarla (Administrador > Torneos) |
| Torneo Recreativo de Padel Dobles | Finalizado | Ver la tabla y el historial. Ganaron Carlos y Diana: cada uno tiene un bono del 15% (Tienda y cafeteria > comprar > usar bono) |
| Copa Relampago de Tenis de Mesa | Finalizado | Competitivo con un empleado (`max`): no pago inscripcion y no recibe premio; Felipe gano $28.000, el 70% de lo recaudado (Torneos > Ver mis bonos y premios) |

Reglas de torneos: los cupos preferenciales (20% redondeado hacia arriba) se liberan 3 dias antes del inicio;
el ganador de un recreativo recibe un bono del 15% y el de un competitivo el 70% de lo recaudado.

Estudiantes y externos: el administrador los registra en Usuarios y turnos; entran por `ConsolaSocio` y solo ven Torneos y la tienda.
`sebas` (estudiante) y `pablo` (externo) ya estan inscritos en el Abierto Competitivo de Tenis.


Si quiere volver a los datos originales despues de probar: `git restore datos/club.json`
