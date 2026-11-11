package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Disciplina;
import cds.academias.Instalacion;
import cds.academias.Modalidad;
import cds.academias.Reserva;
import cds.academias.TipoInstalacion;
import cds.usuarios.Socio;

@DisplayName("HU-JUG-01 Reservar una cancha o mesa de deporte individual")
class HuJug01ReservaDeporteIndividualTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalTime OCHO = LocalTime.of(8, 0);
	private static final LocalTime NUEVE = LocalTime.of(9, 0);

	private Socio jugador(String login) {
		return new Socio(login, "1", login, "CC-" + login, HOY.minusYears(20));
	}

	private Disciplina disciplinaDe(TipoInstalacion tipo) {
		switch (tipo) {
		case CANCHA_PADEL:
			return Disciplina.PADEL;
		case MESA_TENIS_MESA:
			return Disciplina.TENIS_MESA;
		default:
			return Disciplina.TENIS;
		}
	}

	@Nested
	@DisplayName("CA-1 hay instalacion libre y capacidad compatible")
	class ReservaExitosa {

		@ParameterizedTest(name = "{0} en {1} con {2} jugadores")
		@CsvSource({ "CANCHA_TENIS, SENCILLOS, 2", "CANCHA_TENIS, DOBLES, 4", "MESA_TENIS_MESA, SENCILLOS, 2",
				"MESA_TENIS_MESA, DOBLES, 4", "CANCHA_PADEL, DOBLES, 4" })
		@DisplayName("asigna la instalacion")
		void asignaInstalacion(TipoInstalacion tipo, Modalidad modalidad, int jugadores) throws ReglaNegocioException {
			Academia academia = new Academia("Academia", disciplinaDe(tipo), 2);
			Instalacion instalacion = new Instalacion("Instalacion 1", tipo, true);
			academia.agregarInstalacion(instalacion);

			Reserva r = academia.reservar(instalacion, jugador("lucho"), HOY, OCHO, NUEVE, jugadores, modalidad);

			assertSame(instalacion, r.getInstalacion());
			assertTrue(academia.getReservas().contains(r));
		}

		@Test
		@DisplayName("descuenta el cupo simultaneo de la disciplina")
		void descuentaCupo() throws ReglaNegocioException {
			Academia tenis = new Academia("Tenis", Disciplina.TENIS, 1);
			Instalacion cancha1 = new Instalacion("Cancha 1", TipoInstalacion.CANCHA_TENIS, false);
			Instalacion cancha2 = new Instalacion("Cancha 2", TipoInstalacion.CANCHA_TENIS, false);
			tenis.agregarInstalacion(cancha1);
			tenis.agregarInstalacion(cancha2);

			tenis.reservar(cancha1, jugador("lucho"), HOY, OCHO, NUEVE, 2, Modalidad.SENCILLOS);

			assertThrows(ReglaNegocioException.class,
					() -> tenis.reservar(cancha2, jugador("juancho"), HOY, OCHO, NUEVE, 2, Modalidad.SENCILLOS));
		}
	}

	@Nested
	@DisplayName("CA-2 maximo de instalaciones simultaneas ocupado")
	class SinCupo {

		@Test
		@DisplayName("rechaza la reserva")
		void rechazaReserva() throws ReglaNegocioException {
			Academia tenis = new Academia("Tenis", Disciplina.TENIS, 2);
			Instalacion[] canchas = new Instalacion[3];
			for (int i = 0; i < 3; i++) {
				canchas[i] = new Instalacion("Cancha " + i, TipoInstalacion.CANCHA_TENIS, false);
				tenis.agregarInstalacion(canchas[i]);
			}
			tenis.reservar(canchas[0], jugador("lucho"), HOY, OCHO, NUEVE, 2, Modalidad.SENCILLOS);
			tenis.reservar(canchas[1], jugador("juancho"), HOY, OCHO, NUEVE, 2, Modalidad.SENCILLOS);

			assertThrows(ReglaNegocioException.class,
					() -> tenis.reservar(canchas[2], jugador("nico"), HOY, OCHO, NUEVE, 2, Modalidad.SENCILLOS));
			assertEquals(2, tenis.getReservas().size());
		}
	}

	@Nested
	@DisplayName("CA-3 jugador con bebida caliente en padel o tenis de mesa")
	class BebidaCaliente {

		@ParameterizedTest(name = "{0}")
		@CsvSource({ "CANCHA_PADEL, DOBLES, 4", "MESA_TENIS_MESA, SENCILLOS, 2" })
		@DisplayName("no permite usar la instalacion")
		void noPermiteUso(TipoInstalacion tipo, Modalidad modalidad, int jugadores) {
			Academia academia = new Academia("Academia", disciplinaDe(tipo), 2);
			Instalacion instalacion = new Instalacion("Instalacion 1", tipo, true);
			academia.agregarInstalacion(instalacion);
			Socio lucho = jugador("lucho");
			lucho.marcarBebidaCaliente();

			assertThrows(ReglaNegocioException.class,
					() -> academia.reservar(instalacion, lucho, HOY, OCHO, NUEVE, jugadores, modalidad));
			assertTrue(academia.getReservas().isEmpty());
		}
	}
}
