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
import cds.torneos.Encuentro;
import cds.torneos.EstadoTorneo;
import cds.torneos.Inscripcion;
import cds.torneos.JugadorClub;
import cds.torneos.Torneo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Socio;

@DisplayName("HU-SOC-04 Consultar torneos")
class HuSoc04ConsultaTorneosTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);

	private Club club;
	private Torneo abierto;
	private Torneo enCurso;
	private Torneo finalizado;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		abierto = nuevo("Abierto");
		enCurso = nuevo("En curso");
		finalizado = nuevo("Finalizado");
		for (Torneo t : List.of(abierto, enCurso, finalizado)) {
			for (String login : List.of("lucho", "juancho", "nico", "pipe")) {
				t.inscribir(List.of(new JugadorClub(new Socio(login, "1", login, "CC-" + login, HOY.minusYears(25)))),
						false, HOY);
			}
		}
		enCurso.iniciar(INICIO);
		finalizado.iniciar(INICIO);
		while (!finalizado.getEncuentrosPendientes().isEmpty()) {
			Encuentro e = finalizado.getEncuentrosPendientes().get(0);
			finalizado.registrarResultado(e, e.getLocal(), "6-3 6-2");
		}
	}

	private Torneo nuevo(String nombre) throws ReglaNegocioException {
		Torneo t = new TorneoRecreativo(nombre, Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO);
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
			assertEquals(3, finalizado.getEncuentros().size());
			assertTrue(finalizado.getEncuentros().stream().allMatch(Encuentro::isJugado));

			Inscripcion campeon = finalizado.getCampeon();
			assertEquals("lucho", campeon.getIntegrantes().get(0).getId());
			assertEquals("Campeon", finalizado.posicion(campeon));
			assertEquals("Subcampeon", finalizado.posicion(finalizado.getInscripciones().get(2)));
			assertEquals("Ronda 1", finalizado.posicion(finalizado.getInscripciones().get(1)));
		}
	}
}
