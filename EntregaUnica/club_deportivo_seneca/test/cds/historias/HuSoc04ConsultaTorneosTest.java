package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.torneos.EncuentroTorneo;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.EstadoTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.Posicion;
import cds.torneos.Torneo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Socio;

@DisplayName("HU-SOC-04 Consultar torneos")
class HuSoc04ConsultaTorneosTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);
	private static final LocalDate LIMITE = INICIO.minusDays(3);

	private Club club;
	private Torneo finalizado;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		Torneo abierto = nuevo("Abierto");
		Torneo enCurso = nuevo("En curso");
		finalizado = nuevo("Finalizado");
		for (Torneo t : List.of(abierto, enCurso, finalizado)) {
			for (String login : List.of("lucho", "juancho", "nico", "pipe")) {
				club.inscribirEnTorneo(t, new JugadorTorneo(new Socio(login, "1", login, "CC-" + login, HOY.minusYears(25))),
						HOY);
			}
		}
		enCurso.iniciar(INICIO);
		finalizado.iniciar(INICIO);
		List<?> orden = finalizado.getInscripciones();
		for (EncuentroTorneo e : finalizado.generarTodosContraTodos(INICIO)) {
			boolean ganaA = orden.indexOf(e.getParticipanteA()) < orden.indexOf(e.getParticipanteB());
			finalizado.registrarResultado(e, ganaA ? 6 : 3, ganaA ? 3 : 6);
		}
		finalizado.finalizar();
	}

	private Torneo nuevo(String nombre) throws ReglaNegocioException {
		Torneo t = new TorneoRecreativo(nombre, new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, false), 8,
				INICIO, LIMITE, 15);
		club.registrarTorneo(t);
		return t;
	}

	@Nested
	@DisplayName("CA-1 filtrar por estado")
	class FiltrarPorEstado {

		@ParameterizedTest(name = "{0}")
		@EnumSource(EstadoTorneo.class)
		@DisplayName("solo aparecen los torneos en ese estado")
		void filtra(EstadoTorneo estado) {
			List<Torneo> resultado = club.torneosPorEstado(estado);

			assertEquals(1, resultado.size());
			assertEquals(estado, resultado.get(0).getEstado());
		}
	}

	@Nested
	@DisplayName("CA-2 torneo finalizado")
	class HistorialFinalizado {

		@Test
		@DisplayName("muestra sus encuentros, posiciones y campeon")
		void muestraHistorial() {
			assertEquals(6, finalizado.getEncuentros().size());
			assertTrue(finalizado.getEncuentros().stream().allMatch(EncuentroTorneo::isJugado));

			List<Posicion> tabla = finalizado.tablaPosiciones();
			assertEquals("lucho", tabla.get(0).getParticipante().getNombre());
			assertEquals(3, tabla.get(0).getGanados());
			assertEquals("pipe", tabla.get(3).getParticipante().getNombre());
			assertEquals(tabla.get(0).getInscripcion(), finalizado.getGanador());
		}
	}
}
