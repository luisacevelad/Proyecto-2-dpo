package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.torneos.Encuentro;
import cds.torneos.EstadoTorneo;
import cds.torneos.Inscripcion;
import cds.torneos.JugadorClub;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Entrenador;
import cds.usuarios.Socio;
import cds.usuarios.Usuario;

@DisplayName("HU-ADM-06 Registrar resultados")
class HuAdm06ResultadosTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);

	private Socio socio(String login) {
		return new Socio(login, "1", login, "CC-" + login, HOY.minusYears(25));
	}

	private void inscribir(Torneo t, Usuario... jugadores) throws ReglaNegocioException {
		for (Usuario u : jugadores) {
			t.inscribir(List.of(new JugadorClub(u)), false, HOY);
		}
	}

	/** Juega todo el cuadro haciendo ganar siempre al primero de cada encuentro. */
	private void jugarTodo(Torneo t) throws ReglaNegocioException {
		while (!t.getEncuentrosPendientes().isEmpty()) {
			Encuentro e = t.getEncuentrosPendientes().get(0);
			t.registrarResultado(e, e.getLocal(), "6-4 6-4");
		}
	}

	@Nested
	@DisplayName("CA-1 iniciar con 2 o mas inscritos")
	class Iniciar {

		@Test
		@DisplayName("genera los encuentros de la primera ronda y pasa a En curso")
		void generaEncuentros() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Copa", Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO);
			inscribir(t, socio("lucho"), socio("juancho"), socio("nico"), socio("pipe"));

			t.iniciar(INICIO);

			assertEquals(EstadoTorneo.EN_CURSO, t.getEstado());
			assertEquals(2, t.getEncuentrosPendientes().size());
		}

		@Test
		@DisplayName("rechaza iniciar con menos de 2 inscritos")
		void pocosInscritos() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Copa", Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO);
			inscribir(t, socio("lucho"));

			assertThrows(ReglaNegocioException.class, () -> t.iniciar(INICIO));
			assertEquals(EstadoTorneo.ABIERTO, t.getEstado());
		}

		@Test
		@DisplayName("con un numero impar de inscritos, uno pasa directo de ronda")
		void numeroImpar() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Copa", Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO);
			inscribir(t, socio("lucho"), socio("juancho"), socio("nico"));

			t.iniciar(INICIO);
			jugarTodo(t);

			assertEquals(EstadoTorneo.FINALIZADO, t.getEstado());
		}
	}

	@Nested
	@DisplayName("CA-2 final de un competitivo ganada por un socio")
	class CompetitivoSocio {

		@Test
		@DisplayName("el torneo finaliza y el socio recibe el 70% de lo recaudado")
		void recibePremio() throws ReglaNegocioException {
			TorneoCompetitivo t = new TorneoCompetitivo("Abierto", Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO,50000);
			Socio lucho = socio("lucho");
			inscribir(t, lucho, socio("juancho"), socio("nico"), socio("pipe"));
			t.iniciar(INICIO);

			jugarTodo(t);

			assertEquals(EstadoTorneo.FINALIZADO, t.getEstado());
			assertEquals("lucho", t.getCampeon().getIntegrantes().get(0).getId());
			assertEquals(200000, t.totalRecaudado());
			assertEquals(140000, t.getPremioEntregado());
		}
	}

	@Nested
	@DisplayName("CA-3 final de un recreativo")
	class Recreativo {

		@Test
		@DisplayName("cada ganador con cuenta recibe un bono del 15%")
		void recibeBono() throws ReglaNegocioException {
			Torneo t = new TorneoRecreativo("Dobles", Disciplina.PADEL, Nivel.PRINCIPIANTE, true, 8, INICIO);
			Socio lucho = socio("lucho");
			Socio juancho = socio("juancho");
			t.inscribir(List.of(new JugadorClub(lucho), new JugadorClub(juancho)), false, HOY);
			t.inscribir(List.of(new JugadorClub(socio("nico")), new JugadorClub(socio("pipe"))), false, HOY);
			t.iniciar(INICIO);

			jugarTodo(t);

			for (Socio ganador : List.of(lucho, juancho)) {
				assertEquals(1, ganador.getBonos().size());
				assertEquals(15, ganador.getBonos().get(0).getPorcentaje());
				assertEquals("Dobles", ganador.getBonos().get(0).getTorneo());
			}
		}
	}

	@Nested
	@DisplayName("CA-4 final de un competitivo ganada por un empleado")
	class CompetitivoEmpleado {

		@Test
		@DisplayName("no se entrega premio en efectivo")
		void sinPremio() throws ReglaNegocioException {
			TorneoCompetitivo t = new TorneoCompetitivo("Abierto", Disciplina.TENIS, Nivel.INTERMEDIO, false, 8, INICIO,
					50000);
			Entrenador profe = new Entrenador("profe", "1", "Profe", "D1");
			inscribir(t, profe, socio("juancho"), socio("nico"), socio("pipe"));
			t.iniciar(INICIO);

			jugarTodo(t);

			Inscripcion campeon = t.getCampeon();
			assertSame(profe, ((JugadorClub) campeon.getIntegrantes().get(0)).getUsuario());
			assertEquals(150000, t.totalRecaudado());
			assertEquals(0, t.getPremioEntregado());
			assertTrue(t.getEncuentrosPendientes().isEmpty());
		}
	}
}
