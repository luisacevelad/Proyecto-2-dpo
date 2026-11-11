package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import cds.ReglaNegocioException;
import cds.competencias.Liga;
import cds.competencias.NominaCompetencia;
import cds.competencias.PartidoCompetencia;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.partidos.EstadisticaFutbol;
import cds.usuarios.Socio;

@DisplayName("HU-ENT-01 Definir la alineacion para un partido oficial")
class HuEnt01AlineacionTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

	private Socio lucho;
	private Socio juancho;
	private Socio nico;
	private NominaCompetencia nomina;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		lucho = mayor("lucho");
		juancho = mayor("juancho");
		nico = mayor("nico");
		Equipo equipo = new Equipo("Seneca FC", DeporteConjunto.FUTBOL, CategoriaEdad.MAYORES, 20);
		equipo.agregarJugador(lucho, "Delantera", 9, HOY);
		equipo.agregarJugador(juancho, "Defensa", 4, HOY);
		equipo.agregarJugador(nico, "Arquera", 1, HOY);
		Liga liga = new Liga("Liga Universitaria", "ASCUN", 2026, DeporteConjunto.FUTBOL, CategoriaEdad.MAYORES, 2, 3, 1);
		nomina = liga.inscribirEquipo(equipo, HOY, List.of(lucho, juancho));
	}

	private Socio mayor(String login) {
		return new Socio(login, "1", login, "CC-" + login, HOY.minusYears(20));
	}

	private PartidoCompetencia nuevoPartido(int diasDespues) {
		return new PartidoCompetencia(HOY.plusDays(diasDespues), nomina, "Rival", "Cancha 1", DeporteConjunto.FUTBOL);
	}

	@Nested
	@DisplayName("CA-1 jugadores en nomina y sin sanciones")
	class AlineacionValida {

		@Test
		@DisplayName("registra la alineacion para el partido")
		void registraAlineacion() throws ReglaNegocioException {
			PartidoCompetencia partido = nuevoPartido(1);

			partido.confirmarAlineacion(List.of(lucho, juancho));

			assertEquals(List.of(lucho, juancho), partido.getParticipantes());
		}
	}

	@Nested
	@DisplayName("CA-2 jugador fuera de la nomina")
	class JugadorFueraDeNomina {

		@Test
		@DisplayName("rechaza toda la alineacion e informa que no esta habilitado para el torneo")
		void rechazaAlineacion() {
			PartidoCompetencia partido = nuevoPartido(1);

			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> partido.confirmarAlineacion(List.of(lucho, nico)));

			assertTrue(ex.getMessage().contains("no esta habilitado para el torneo"), ex.getMessage());
			assertTrue(partido.getParticipantes().isEmpty());
		}
	}

	@Nested
	@DisplayName("CA-3 jugador suspendido por tarjetas")
	class JugadorSuspendido {

		@ParameterizedTest(name = "{0} amarillas y {1} rojas en un partido")
		@CsvSource({ "2, 0", "0, 1" })
		@DisplayName("rechaza alinearlo en el siguiente partido e informa que esta suspendido")
		void suspendidoPorUnPartido(int amarillas, int rojas) throws ReglaNegocioException {
			PartidoCompetencia primero = nuevoPartido(1);
			primero.confirmarAlineacion(List.of(lucho, juancho));
			primero.registrarEstadistica(new EstadisticaFutbol(lucho, 0, 0, amarillas, rojas));
			primero.finalizar(1, 0);

			PartidoCompetencia segundo = nuevoPartido(8);
			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> segundo.confirmarAlineacion(List.of(lucho, juancho)));

			assertTrue(ex.getMessage().contains("suspendido"), ex.getMessage());
			assertTrue(segundo.getParticipantes().isEmpty());
		}

		@Test
		@DisplayName("acumular dos amarillas en partidos distintos tambien lo suspende")
		void amarillasAcumuladas() throws ReglaNegocioException {
			for (int i = 1; i <= 2; i++) {
				PartidoCompetencia p = nuevoPartido(i * 7);
				p.confirmarAlineacion(List.of(lucho, juancho));
				p.registrarEstadistica(new EstadisticaFutbol(lucho, 0, 0, 1, 0));
				p.finalizar(1, 1);
			}

			PartidoCompetencia tercero = nuevoPartido(21);
			ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
					() -> tercero.confirmarAlineacion(List.of(lucho, juancho)));

			assertTrue(ex.getMessage().contains("suspendido"), ex.getMessage());
		}
	}
}
