package cds.requerimientos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Disciplina;
import cds.academias.Modalidad;
import cds.academias.Nivel;
import cds.academias.PartidoPractica;
import cds.competencias.Liga;
import cds.competencias.NominaCompetencia;
import cds.competencias.PartidoCompetencia;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.conjunto.MiembroEquipo;
import cds.conjunto.PartidoInterno;
import cds.partidos.EstadisticaBaloncesto;
import cds.usuarios.Socio;

/** Requerimientos de partidos internos, ligas y partidos de practica con ranking. */
@DisplayName("RF Partidos, ligas y ranking")
class CompetenciasYPartidosTest {
	private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

	private Club club;
	private int documentos;

	@BeforeEach
	void setUp() {
		club = new Club();
		documentos = 0;
	}

	private Socio socio(String login) throws ReglaNegocioException {
		Socio s = new Socio(login, "1", login, "D" + (documentos++), LocalDate.of(2000, 1, 1));
		club.registrarUsuario(s);
		return s;
	}

	private Equipo equipoDeBaloncesto(String nombre, int jugadores) throws ReglaNegocioException {
		Equipo e = new Equipo(nombre, DeporteConjunto.BALONCESTO, CategoriaEdad.MAYORES, 12);
		club.registrarEquipo(e);
		for (int i = 1; i <= jugadores; i++) {
			club.inscribirJugador(e, socio(nombre + i), "Base", i, HOY);
		}
		return e;
	}

	private static List<Socio> jugadores(Equipo e) {
		List<Socio> lista = new ArrayList<Socio>();
		for (MiembroEquipo m : e.getMiembros()) {
			lista.add(m.getSocio());
		}
		return lista;
	}

	@Nested
	@DisplayName("Partidos internos")
	class Internos {

		@Test
		@DisplayName("exige el minimo de jugadores habilitados en ambos equipos")
		void minimoDeJugadores() throws ReglaNegocioException {
			Equipo completo = equipoDeBaloncesto("A", 5);
			Equipo incompleto = equipoDeBaloncesto("B", 4);
			assertThrows(ReglaNegocioException.class, () -> club.registrarPartidoInterno(completo, incompleto, HOY));
		}

		@Test
		@DisplayName("se registra con marcador y queda en los partidos jugados")
		void registraResultado() throws ReglaNegocioException {
			Equipo a = equipoDeBaloncesto("A", 5);
			Equipo b = equipoDeBaloncesto("B", 5);
			PartidoInterno p = club.registrarPartidoInterno(a, b, HOY);
			p.agregarParticipante(jugadores(a).get(0));
			p.registrarEstadistica(new EstadisticaBaloncesto(jugadores(a).get(0), 20, 5));
			p.finalizar(80, 70);
			assertEquals(1, club.partidosJugados().size());
			assertEquals(80, p.getMarcadorLocal());
		}

		@Test
		@DisplayName("en baloncesto no puede haber empate")
		void sinEmpate() throws ReglaNegocioException {
			PartidoInterno p = club.registrarPartidoInterno(equipoDeBaloncesto("A", 5), equipoDeBaloncesto("B", 5), HOY);
			assertThrows(ReglaNegocioException.class, () -> p.finalizar(70, 70));
		}
	}

	@Nested
	@DisplayName("Ligas: la tabla se actualiza sola al finalizar un partido oficial")
	class Ligas {
		private Liga liga;
		private Equipo equipo;
		private NominaCompetencia nomina;

		@BeforeEach
		void liga() throws ReglaNegocioException {
			liga = new Liga("Liga U", "ASCUN", 2026, DeporteConjunto.BALONCESTO, CategoriaEdad.MAYORES, 5, 2, 0);
			club.registrarCompetencia(liga);
			equipo = equipoDeBaloncesto("Rojo", 5);
			nomina = liga.inscribirEquipo(equipo, HOY, jugadores(equipo));
		}

		private PartidoCompetencia jugar(int nuestro, int rival) throws ReglaNegocioException {
			PartidoCompetencia p = new PartidoCompetencia(HOY, nomina, "Rival", "Coliseo", DeporteConjunto.BALONCESTO);
			p.confirmarAlineacion(jugadores(equipo));
			p.finalizar(nuestro, rival);
			return p;
		}

		@Test
		@DisplayName("el partido conoce su competencia")
		void conoceSuCompetencia() throws ReglaNegocioException {
			PartidoCompetencia p = new PartidoCompetencia(HOY, nomina, "Rival", "Coliseo", DeporteConjunto.BALONCESTO);
			assertSame(liga, p.getComeptencia());
		}

		@Test
		@DisplayName("una victoria suma los puntos de victoria de la liga")
		void victoria() throws ReglaNegocioException {
			jugar(80, 60);
			assertEquals(2, liga.getPuntos(equipo));
		}

		@Test
		@DisplayName("las derrotas no suman y las victorias se acumulan")
		void acumula() throws ReglaNegocioException {
			jugar(80, 60);
			jugar(50, 60);
			jugar(70, 65);
			assertEquals(4, liga.getPuntos(equipo));
		}
	}

	@Nested
	@DisplayName("Partidos de practica y ranking de academia")
	class Practica {
		private Academia tenis;
		private Socio ana;
		private Socio beto;

		@BeforeEach
		void academia() throws ReglaNegocioException {
			tenis = new Academia("Tenis", Disciplina.TENIS, 2);
			club.registrarAcademia(tenis);
			ana = socio("ana");
			beto = socio("beto");
			tenis.inscribirSocio(ana, Nivel.INTERMEDIO, HOY);
			tenis.inscribirSocio(beto, Nivel.INTERMEDIO, HOY);
		}

		@Test
		@DisplayName("el ganador sube de ranking, el perdedor baja y queda en el historial")
		void actualizaRanking() throws ReglaNegocioException {
			PartidoPractica p = new PartidoPractica(HOY, tenis, Modalidad.SENCILLOS);
			p.agregarParticipante(ana);
			p.agregarParticipante(beto);
			club.registrarPartido(p);
			p.finalizar(2, 0);
			assertTrue(tenis.getInscripcion(ana).getPuntajeRanking() > 1000);
			assertTrue(tenis.getInscripcion(beto).getPuntajeRanking() < 1000);
			assertEquals(1, club.evolucionRanking(tenis, ana).size());
		}

		@Test
		@DisplayName("no se enfrentan niveles con mas de un nivel de diferencia")
		void nivelesIncompatibles() throws ReglaNegocioException {
			Socio novato = socio("novato");
			Socio experto = socio("experto");
			tenis.inscribirSocio(novato, Nivel.PRINCIPIANTE, HOY);
			tenis.inscribirSocio(experto, Nivel.AVANZADO, HOY);
			PartidoPractica p = new PartidoPractica(HOY, tenis, Modalidad.SENCILLOS);
			p.agregarParticipante(novato);
			assertThrows(ReglaNegocioException.class, () -> p.agregarParticipante(experto));
		}
	}
}
