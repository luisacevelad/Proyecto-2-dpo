package cds.requerimientos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Disciplina;
import cds.academias.Instalacion;
import cds.academias.Modalidad;
import cds.academias.Nivel;
import cds.academias.TipoInstalacion;
import cds.usuarios.Administrador;
import cds.usuarios.Entrenador;
import cds.usuarios.EstadoSolicitud;
import cds.usuarios.SolicitudCambioTurno;
import cds.usuarios.Turno;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.Socio;

/** Reglas de operacion del club que valida el modelo (y no la consola). */
@DisplayName("RF Reservas, turnos, codigos e instalaciones")
class ReglasDeOperacionTest {
	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 12, 10, 0);
	private static final LocalDate MANANA = LocalDate.of(2026, 11, 13);

	private Club club;
	private Academia tenis;
	private Instalacion cancha;
	private Socio ana;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		tenis = new Academia("Tenis", Disciplina.TENIS, 2);
		club.registrarAcademia(tenis);
		cancha = new Instalacion("Cancha 1", TipoInstalacion.CANCHA_TENIS, false);
		club.agregarInstalacion(tenis, cancha);
		ana = new Socio("ana", "1", "Ana", "D1", LocalDate.of(2000, 1, 1));
		club.registrarUsuario(ana);
		tenis.inscribirSocio(ana, Nivel.INTERMEDIO, AHORA.toLocalDate());
	}

	@Nested
	@DisplayName("Reservas")
	class Reservas {

		@Test
		@DisplayName("reserva dentro del horario del club en una fecha futura")
		void reservaValida() throws ReglaNegocioException {
			club.reservar(tenis, cancha, ana, MANANA, LocalTime.of(8, 0), LocalTime.of(9, 0), 2, Modalidad.SENCILLOS, AHORA);
			assertEquals(1, tenis.getReservas().size());
		}

		@Test
		@DisplayName("rechaza fechas pasadas y horas que ya pasaron hoy")
		void enElPasado() {
			assertThrows(ReglaNegocioException.class, () -> club.reservar(tenis, cancha, ana, AHORA.toLocalDate().minusDays(1),
					LocalTime.of(8, 0), LocalTime.of(9, 0), 2, Modalidad.SENCILLOS, AHORA));
			assertThrows(ReglaNegocioException.class, () -> club.reservar(tenis, cancha, ana, AHORA.toLocalDate(),
					LocalTime.of(9, 0), LocalTime.of(10, 0), 2, Modalidad.SENCILLOS, AHORA));
		}

		@Test
		@DisplayName("rechaza horas fuera de la atencion del club (6:00 a 22:00)")
		void fueraDeHorario() {
			assertThrows(ReglaNegocioException.class, () -> club.reservar(tenis, cancha, ana, MANANA, LocalTime.of(5, 0),
					LocalTime.of(6, 30), 2, Modalidad.SENCILLOS, AHORA));
			assertThrows(ReglaNegocioException.class, () -> club.reservar(tenis, cancha, ana, MANANA, LocalTime.of(21, 30),
					LocalTime.of(22, 30), 2, Modalidad.SENCILLOS, AHORA));
		}
	}

	@Nested
	@DisplayName("Turnos")
	class Turnos {

		@Test
		@DisplayName("asigna un turno valido")
		void turnoValido() throws ReglaNegocioException {
			Entrenador leo = new Entrenador("leo", "1", "Leo", "LEO");
			club.asignarTurno(leo, DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(14, 0));
			assertEquals(1, club.getTurnos().size());
		}

		@Test
		@DisplayName("rechaza horas invertidas o fuera del horario del club")
		void turnoInvalido() {
			Entrenador leo = new Entrenador("leo", "1", "Leo", "LEO");
			assertThrows(ReglaNegocioException.class,
					() -> club.asignarTurno(leo, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(8, 0)));
			assertThrows(ReglaNegocioException.class,
					() -> club.asignarTurno(leo, DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(23, 0)));
		}
	}

	@Nested
	@DisplayName("Autorizacion de operaciones administrativas")
	class Autorizacion {

		@Test
		@DisplayName("un administrador que no esta registrado en el club no puede resolver solicitudes")
		void administradorAjeno() throws ReglaNegocioException {
			Entrenador leo = new Entrenador("leo", "1", "Leo", "LEO");
			club.registrarUsuario(leo);
			Turno turno = club.asignarTurno(leo, DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(14, 0));
			SolicitudCambioTurno s = club.solicitarCambioTurno(leo, turno, DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.of(14, 0));
			Administrador intruso = new Administrador("admin", "x", "Intruso");
			assertThrows(ReglaNegocioException.class, () -> club.resolverSolicitud(intruso, s, true));
			assertThrows(ReglaNegocioException.class, () -> club.resolverSolicitud(null, s, true));
			assertEquals(EstadoSolicitud.PENDIENTE, s.getEstado());
		}

		@Test
		@DisplayName("el administrador autenticado si puede resolverlas")
		void administradorDelClub() throws ReglaNegocioException {
			Administrador admin = new Administrador("admin", "admin123", "Admin");
			club.registrarUsuario(admin);
			Entrenador leo = new Entrenador("leo", "1", "Leo", "LEO");
			club.registrarUsuario(leo);
			Turno turno = club.asignarTurno(leo, DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(14, 0));
			SolicitudCambioTurno s = club.solicitarCambioTurno(leo, turno, DayOfWeek.TUESDAY, LocalTime.of(6, 0), LocalTime.of(14, 0));
			club.resolverSolicitud((Administrador) club.autenticar("admin", "admin123"), s, false);
			assertEquals(EstadoSolicitud.RECHAZADA, s.getEstado());
		}
	}

	@Nested
	@DisplayName("Codigos de descuento e instalaciones")
	class Unicidad {

		@Test
		@DisplayName("encuentra al dueno de un codigo sin importar mayusculas")
		void buscaCodigo() throws ReglaNegocioException {
			Entrenador leo = new Entrenador("leo", "1", "Leo", "LEO-CDS");
			club.registrarUsuario(leo);
			assertSame(leo, club.buscarEmpleadoPorCodigo("leo-cds"));
			assertNull(club.buscarEmpleadoPorCodigo("NO-EXISTE"));
		}

		@Test
		@DisplayName("dos empleados no pueden tener el mismo codigo")
		void codigoDuplicado() throws ReglaNegocioException {
			club.registrarUsuario(new Entrenador("leo", "1", "Leo", "LEO-CDS"));
			assertThrows(ReglaNegocioException.class,
					() -> club.registrarUsuario(new Fisioterapeuta("otro", "1", "Otro", "leo-cds")));
		}

		@Test
		@DisplayName("no se repite el nombre de una instalacion en todo el club")
		void instalacionDuplicada() throws ReglaNegocioException {
			Academia padel = new Academia("Padel", Disciplina.PADEL, 1);
			club.registrarAcademia(padel);
			assertThrows(ReglaNegocioException.class,
					() -> club.agregarInstalacion(padel, new Instalacion("cancha 1", TipoInstalacion.CANCHA_PADEL, true)));
		}
	}
}
