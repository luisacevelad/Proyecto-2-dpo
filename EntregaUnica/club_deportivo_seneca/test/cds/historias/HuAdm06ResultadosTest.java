package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.torneos.EncuentroTorneo;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.EstadoTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.ParejaTorneo;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Entrenador;
import cds.usuarios.Socio;
import cds.usuarios.Usuario;
import cds.ventas.Bono;

@DisplayName("HU-ADM-06 Registrar resultados")
class HuAdm06ResultadosTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);
	private static final LocalDate LIMITE = INICIO.minusDays(3);
	private static final EspecialidadIndividual TENIS = new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, false);

	private Club club;

	@BeforeEach
	void setUp() {
		club = new Club();
	}

	private Socio socio(String login) {
		return new Socio(login, "1", login, "CC-" + login, HOY.minusYears(25));
	}

	private void inscribir(Torneo t, Usuario... jugadores) throws ReglaNegocioException {
		for (Usuario u : jugadores) {
			club.inscribirEnTorneo(t, new JugadorTorneo(u), HOY);
		}
	}

	/** Inicia y juega todos contra todos: siempre gana el que se inscribio primero. */
	private void jugarTodo(Torneo t) throws ReglaNegocioException {
		t.iniciar(INICIO);
		List<?> orden = t.getInscripciones();
		for (EncuentroTorneo e : t.generarTodosContraTodos(INICIO)) {
			boolean ganaA = orden.indexOf(e.getParticipanteA()) < orden.indexOf(e.getParticipanteB());
			t.registrarResultado(e, ganaA ? 2 : 0, ganaA ? 0 : 2);
		}
		t.finalizar();
	}

	@Nested
	@DisplayName("CA-1 iniciar con 2 o mas inscritos")
	class Iniciar {

		@Test
		@DisplayName("genera los encuentros y pasa a En curso")
		void generaEncuentros() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Copa", TENIS, 8, INICIO, LIMITE, 15);
			club.registrarTorneo(t);
			inscribir(t, socio("lucho"), socio("juancho"), socio("nico"), socio("pipe"));

			t.iniciar(INICIO);

			assertEquals(EstadoTorneo.EN_CURSO, t.getEstado());
			assertEquals(6, t.generarTodosContraTodos(INICIO).size());
		}

		@Test
		@DisplayName("rechaza iniciar con menos de 2 inscritos")
		void pocosInscritos() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Copa", TENIS, 8, INICIO, LIMITE, 15);
			club.registrarTorneo(t);
			inscribir(t, socio("lucho"));

			assertThrows(ReglaNegocioException.class, () -> t.iniciar(INICIO));
			assertEquals(EstadoTorneo.INSCRIPCIONES_ABIERTAS, t.getEstado());
		}
	}

	@Nested
	@DisplayName("CA-2 competitivo ganado por un socio")
	class CompetitivoSocio {

		@Test
		@DisplayName("el torneo finaliza y el socio recibe lo recaudado")
		void recibePremio() throws ReglaNegocioException {
			TorneoCompetitivo t = new TorneoCompetitivo("Abierto", TENIS, 8, INICIO, LIMITE, 50000);
			club.registrarTorneo(t);
			Socio lucho = socio("lucho");
			inscribir(t, lucho, socio("juancho"), socio("nico"), socio("pipe"));

			jugarTodo(t);

			assertEquals(EstadoTorneo.FINALIZADO, t.getEstado());
			assertTrue(t.getGanador().getParticipante().incluye(lucho));
			assertEquals(200000, t.totalRecaudado());
			assertEquals(200000, t.getPremiosEntregados().get(lucho));
		}
	}

	@Nested
	@DisplayName("CA-3 recreativo")
	class Recreativo {

		@Test
		@DisplayName("cada ganador recibe un bono del porcentaje del torneo")
		void recibeBono() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Dobles", new EspecialidadIndividual(Disciplina.PADEL, Nivel.PRINCIPIANTE, true), 8,
					INICIO, LIMITE, 15);
			club.registrarTorneo(t);
			Socio lucho = socio("lucho");
			Socio juancho = socio("juancho");
			club.inscribirEnTorneo(t, new ParejaTorneo(lucho, juancho), HOY);
			club.inscribirEnTorneo(t, new ParejaTorneo(socio("nico"), socio("pipe")), HOY);

			jugarTodo(t);

			for (Socio ganador : List.of(lucho, juancho)) {
				List<Bono> bonos = club.bonosDisponibles(ganador);
				assertEquals(1, bonos.size());
				assertEquals(15, bonos.get(0).getPorcentaje());
				assertEquals("Dobles", bonos.get(0).getOrigen());
			}
		}
	}

	@Nested
	@DisplayName("CA-4 competitivo ganado por un empleado")
	class CompetitivoEmpleado {

		@Test
		@DisplayName("no se entrega premio en efectivo")
		void sinPremio() throws ReglaNegocioException {
			TorneoCompetitivo t = new TorneoCompetitivo("Abierto", TENIS, 8, INICIO, LIMITE, 50000);
			club.registrarTorneo(t);
			Entrenador profe = new Entrenador("profe", "1", "Profe", "D1");
			inscribir(t, profe, socio("juancho"), socio("nico"), socio("pipe"));

			jugarTodo(t);

			assertSame(profe, ((JugadorTorneo) t.getGanador().getParticipante()).getJugador());
			assertEquals(150000, t.totalRecaudado());
			assertTrue(t.getPremiosEntregados().isEmpty());
		}
	}
}
