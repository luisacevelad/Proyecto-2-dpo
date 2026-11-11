package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cds.Club;
import cds.ReglaNegocioException;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.conjunto.MiembroEquipo;
import cds.usuarios.Socio;

@DisplayName("HU-SOC-02 Inscribirse en un equipo de conjunto")
class HuSoc02InscripcionEquipoTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

	private Club club;
	private Equipo sub15;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		sub15 = new Equipo("Seneca Sub-15", DeporteConjunto.FUTBOL, CategoriaEdad.SUB_15, 2);
		club.registrarEquipo(sub15);
	}

	private Socio socioDe(String login, int edad) {
		return new Socio(login, "1", login, "TI-" + login, HOY.minusYears(edad));
	}

	@Nested
	@DisplayName("CA-1 cupo, categoria, deporte y dorsal validos")
	class InscripcionExitosa {

		@Test
		@DisplayName("registra al socio con dorsal y posicion y descuenta un cupo")
		void registraSocio() throws ReglaNegocioException {
			Socio lucho = socioDe("lucho", 15);

			club.inscribirJugador(sub15, lucho, "Volante", 8, HOY);

			assertTrue(sub15.tieneJugador(lucho));
			MiembroEquipo miembro = sub15.getMiembros().get(0);
			assertEquals(8, miembro.getNumeroCamiseta());
			assertEquals("Volante", miembro.getPosicion());
			assertEquals(1, sub15.getCupoMaximo() - sub15.getMiembros().size());
		}
	}

	@Nested
	@DisplayName("CA-2 edad no coincide con la categoria")
	class CategoriaIncompatible {

		@ParameterizedTest(name = "{1} anos en {0}")
		@CsvSource({ "SUB_13, 15", "SUB_15, 13", "SUB_15, 16", "SUB_17, 18", "MAYORES, 17" })
		@DisplayName("rechaza la solicitud e indica la incompatibilidad de categoria")
		void rechazaPorCategoria(CategoriaEdad categoria, int edad) throws ReglaNegocioException {
			Equipo equipo = new Equipo("Equipo " + categoria, DeporteConjunto.VOLEIBOL, categoria, 12);
			club.registrarEquipo(equipo);
			Socio socio = socioDe("socio", edad);

			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> club.inscribirJugador(equipo, socio, "Libero", 5, HOY));

			assertTrue(ex.getMessage().contains(categoria.getNombre()), ex.getMessage());
			assertFalse(equipo.tieneJugador(socio));
		}
	}

	@Nested
	@DisplayName("CA-3 dorsal ya asignado")
	class DorsalOcupado {

		@Test
		@DisplayName("rechaza la operacion y pide elegir un dorsal libre")
		void pideDorsalLibre() throws ReglaNegocioException {
			club.inscribirJugador(sub15, socioDe("lucho", 15), "Volante", 10, HOY);
			Socio juancho = socioDe("juancho", 14);

			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> club.inscribirJugador(sub15, juancho, "Delantero", 10, HOY));

			assertTrue(ex.getMessage().contains("elija un dorsal libre"), ex.getMessage());
			assertFalse(sub15.tieneJugador(juancho));
		}
	}

	@Nested
	@DisplayName("CA-4 ya pertenece a otro equipo del mismo deporte")
	class OtroEquipoMismoDeporte {

		@Test
		@DisplayName("deniega el registro e indica que solo puede estar en un equipo por disciplina")
		void deniegaRegistro() throws ReglaNegocioException {
			Equipo otro = new Equipo("Seneca Sub-15 B", DeporteConjunto.FUTBOL, CategoriaEdad.SUB_15, 20);
			club.registrarEquipo(otro);
			Socio lucho = socioDe("lucho", 15);
			club.inscribirJugador(sub15, lucho, "Volante", 8, HOY);

			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> club.inscribirJugador(otro, lucho, "Volante", 8, HOY));

			assertTrue(ex.getMessage().contains("un unico equipo por disciplina"), ex.getMessage());
			assertFalse(otro.tieneJugador(lucho));
		}
	}
}
