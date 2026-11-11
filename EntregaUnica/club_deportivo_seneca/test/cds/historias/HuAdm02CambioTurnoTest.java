package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.usuarios.Administrador;
import cds.usuarios.Empleado;
import cds.usuarios.Entrenador;
import cds.usuarios.EstadoSolicitud;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.SolicitudCambioTurno;
import cds.usuarios.Turno;

@DisplayName("HU-ADM-02 Revisar solicitudes de cambio de turno")
class HuAdm02CambioTurnoTest {

	private static final DayOfWeek LUNES = DayOfWeek.MONDAY;
	private static final DayOfWeek MARTES = DayOfWeek.TUESDAY;

	private Club club;
	private Administrador admin;

	@BeforeEach
	void setUp() {
		club = new Club();
		admin = new Administrador("admin", "123", "Admin");
	}

	private Turno turnoCompleto(Empleado e, DayOfWeek dia) {
		Turno t = new Turno(e, dia, Club.APERTURA, Club.CIERRE);
		club.registrarTurno(t);
		return t;
	}

	private SolicitudCambioTurno pedirPasarAlMartes(Empleado e, Turno t) throws ReglaNegocioException {
		return club.solicitarCambioTurno(e, t, MARTES, Club.APERTURA, Club.CIERRE);
	}

	private void coberturaMinimaDelMartes() {
		turnoCompleto(new Entrenador("e4", "1", "E4", "D4"), MARTES);
		turnoCompleto(new Entrenador("e5", "1", "E5", "D5"), MARTES);
		turnoCompleto(new Fisioterapeuta("f3", "1", "F3", "D6"), MARTES);
	}

	@Nested
	@DisplayName("CA-1 el cambio mantiene 2 entrenadores y 1 fisioterapeuta")
	class CambioAprobado {

		@Test
		@DisplayName("registra el cambio de turno")
		void registraCambio() throws ReglaNegocioException {
			Entrenador e1 = new Entrenador("e1", "1", "E1", "D1");
			turnoCompleto(e1, LUNES);
			turnoCompleto(new Entrenador("e2", "1", "E2", "D2"), LUNES);
			Entrenador e3 = new Entrenador("e3", "1", "E3", "D3");
			Turno turnoE3 = turnoCompleto(e3, LUNES);
			turnoCompleto(new Fisioterapeuta("f1", "1", "F1", "D7"), LUNES);
			coberturaMinimaDelMartes();

			SolicitudCambioTurno s = pedirPasarAlMartes(e3, turnoE3);
			club.resolverSolicitud(admin, s, true);

			assertEquals(EstadoSolicitud.APROBADA, s.getEstado());
			assertEquals(MARTES, turnoE3.getDia());
		}
	}

	@Nested
	@DisplayName("CA-2 el cambio dejaria menos personal del minimo")
	class CambioRechazado {

		@Test
		@DisplayName("rechaza si quedan menos de 2 entrenadores")
		void rechazaPorEntrenadores() throws ReglaNegocioException {
			Entrenador e1 = new Entrenador("e1", "1", "E1", "D1");
			Turno turnoE1 = turnoCompleto(e1, LUNES);
			turnoCompleto(new Entrenador("e2", "1", "E2", "D2"), LUNES);
			turnoCompleto(new Fisioterapeuta("f1", "1", "F1", "D7"), LUNES);
			coberturaMinimaDelMartes();

			SolicitudCambioTurno s = pedirPasarAlMartes(e1, turnoE1);

			assertThrows(ReglaNegocioException.class, () -> club.resolverSolicitud(admin, s, true));
			assertEquals(LUNES, turnoE1.getDia());
			assertEquals(LocalTime.of(6, 0), turnoE1.getHoraInicio());
		}

		@Test
		@DisplayName("rechaza si queda menos de 1 fisioterapeuta")
		void rechazaPorFisioterapeuta() throws ReglaNegocioException {
			turnoCompleto(new Entrenador("e1", "1", "E1", "D1"), LUNES);
			turnoCompleto(new Entrenador("e2", "1", "E2", "D2"), LUNES);
			Fisioterapeuta f1 = new Fisioterapeuta("f1", "1", "F1", "D7");
			Turno turnoF1 = turnoCompleto(f1, LUNES);
			coberturaMinimaDelMartes();

			SolicitudCambioTurno s = pedirPasarAlMartes(f1, turnoF1);

			assertThrows(ReglaNegocioException.class, () -> club.resolverSolicitud(admin, s, true));
			assertEquals(LUNES, turnoF1.getDia());
		}
	}
}
