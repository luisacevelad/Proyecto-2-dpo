package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Disciplina;
import cds.academias.Instalacion;
import cds.academias.Modalidad;
import cds.academias.Reserva;
import cds.academias.TipoInstalacion;
import cds.usuarios.Socio;

@DisplayName("HU-SOC-01 Reservar una instalacion para practica individual")
class HuSoc01ReservaInstalacionTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalTime DIEZ = LocalTime.of(10, 0);
	private static final LocalTime ONCE = LocalTime.of(11, 0);

	private Academia padel;
	private Instalacion cancha1;
	private Instalacion cancha2;

	@BeforeEach
	void setUp() {
		padel = new Academia("Padel", Disciplina.PADEL, 1);
		cancha1 = new Instalacion("Padel 1", TipoInstalacion.CANCHA_PADEL, true);
		cancha2 = new Instalacion("Padel 2", TipoInstalacion.CANCHA_PADEL, true);
		padel.agregarInstalacion(cancha1);
		padel.agregarInstalacion(cancha2);
	}

	private Socio socio(String login) {
		return new Socio(login, "1", login, "CC-" + login, HOY.minusYears(25));
	}

	@Nested
	@DisplayName("CA-1 el club no ha alcanzado el maximo simultaneo")
	class ReservaRegistrada {

		@Test
		@DisplayName("registra la reserva con modalidad y jugadores y la asocia a la instalacion")
		void registraReserva() throws ReglaNegocioException {
			Reserva r = padel.reservar(cancha1, socio("lucho"), HOY, DIEZ, ONCE, 4, Modalidad.DOBLES);

			assertSame(cancha1, r.getInstalacion());
			assertEquals(Modalidad.DOBLES, r.getModalidad());
			assertTrue(padel.getReservas().contains(r));
		}
	}

	@Nested
	@DisplayName("CA-2 todas las instalaciones de la disciplina ocupadas")
	class SinDisponibilidad {

		@Test
		@DisplayName("rechaza la reserva e informa que no hay disponibilidad")
		void rechazaReserva() throws ReglaNegocioException {
			padel.reservar(cancha1, socio("lucho"), HOY, DIEZ, ONCE, 4, Modalidad.DOBLES);

			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> padel.reservar(cancha2, socio("juancho"), HOY, DIEZ, ONCE, 4, Modalidad.DOBLES));

			assertTrue(ex.getMessage().contains("no hay disponibilidad"), ex.getMessage());
			assertEquals(1, padel.getReservas().size());
		}
	}

	@Nested
	@DisplayName("CA-3 padel para 2 jugadores")
	class PadelSoloDobles {

		@ParameterizedTest(name = "modalidad {0}")
		@EnumSource(Modalidad.class)
		@DisplayName("rechaza la reserva porque el padel siempre es dobles")
		void rechazaDosJugadores(Modalidad modalidad) {
			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> padel.reservar(cancha1, socio("lucho"), HOY, DIEZ, ONCE, 2, modalidad));

			assertTrue(ex.getMessage().contains("dobles"), ex.getMessage());
			assertTrue(padel.getReservas().isEmpty());
		}
	}
}
