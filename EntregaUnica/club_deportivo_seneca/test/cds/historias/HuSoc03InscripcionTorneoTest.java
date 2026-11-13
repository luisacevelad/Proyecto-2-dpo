package cds.historias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import cds.academias.Academia;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.InscripcionTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.ParejaTorneo;
import cds.torneos.Torneo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Socio;

@DisplayName("HU-SOC-03 Inscribirse o salirse de un torneo")
class HuSoc03InscripcionTorneoTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);
	private static final LocalDate LIMITE = INICIO.minusDays(3);

	private Club club;
	private Socio lucho;
	private Socio juancho;
	private Socio nico;

	@BeforeEach
	void setUp() throws ReglaNegocioException {
		club = new Club();
		lucho = socio("lucho");
		juancho = socio("juancho");
		nico = socio("nico");
		Academia tenis = new Academia("Academia de tenis", Disciplina.TENIS, 4);
		club.registrarAcademia(tenis);
		for (Socio s : List.of(lucho, juancho, nico)) {
			tenis.inscribirSocio(s, Nivel.INTERMEDIO, HOY);
		}
		tenis.getInscripcion(lucho).actualizarPuntaje(1.0, 1000, HOY);
	}

	private Socio socio(String login) {
		return new Socio(login, "1", login, "CC-" + login, HOY.minusYears(25));
	}

	private Torneo torneo(int cupos) throws ReglaNegocioException {
		Torneo t = new TorneoRecreativo("Copa de tenis", new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, false),
				cupos, INICIO, LIMITE, 15);
		club.registrarTorneo(t);
		return t;
	}

	private InscripcionTorneo inscribir(Torneo t, Socio s, LocalDate fecha) throws ReglaNegocioException {
		return club.inscribirEnTorneo(t, new JugadorTorneo(s), fecha);
	}

	@Nested
	@DisplayName("CA-1 socio en el top del ranking")
	class SocioTop {

		@Test
		@DisplayName("ocupa un cupo preferencial")
		void ocupaPreferencial() throws ReglaNegocioException {
			Torneo t = torneo(5);

			assertTrue(inscribir(t, lucho, HOY).isPreferencial());
			assertFalse(inscribir(t, juancho, HOY).isPreferencial());
		}
	}

	@Nested
	@DisplayName("CA-2 solo quedan cupos preferenciales")
	class SoloPreferenciales {

		@Test
		@DisplayName("rechaza a un socio fuera del top antes de la fecha limite")
		void rechazaFueraDelTop() throws ReglaNegocioException {
			Torneo t = torneo(2);
			inscribir(t, juancho, HOY);

			assertThrows(ReglaNegocioException.class, () -> inscribir(t, nico, HOY));
			assertEquals(1, t.getInscripciones().size());
		}

		@Test
		@DisplayName("despues de la fecha limite los cupos preferenciales se liberan")
		void liberaPorFecha() throws ReglaNegocioException {
			Torneo t = torneo(2);
			inscribir(t, juancho, HOY);

			inscribir(t, nico, LIMITE.plusDays(1));

			assertEquals(2, t.getInscripciones().size());
		}

		@Test
		@DisplayName("el administrador puede liberarlos antes")
		void liberaManual() throws ReglaNegocioException {
			Torneo t = torneo(2);
			inscribir(t, juancho, HOY);

			t.liberarCuposPreferenciales();
			inscribir(t, nico, HOY);

			assertEquals(2, t.getInscripciones().size());
		}

		@Test
		@DisplayName("rechaza inscribir dos veces al mismo socio")
		void rechazaDuplicado() throws ReglaNegocioException {
			Torneo t = torneo(5);
			inscribir(t, juancho, HOY);

			assertThrows(ReglaNegocioException.class, () -> inscribir(t, juancho, HOY));
		}
	}

	@Nested
	@DisplayName("CA-3 socio inscrito y torneo sin iniciar")
	class Salirse {

		@Test
		@DisplayName("al salirse se libera su cupo")
		void liberaCupo() throws ReglaNegocioException {
			Torneo t = torneo(5);
			inscribir(t, lucho, HOY);

			club.desinscribirDeTorneo(t, lucho, HOY);

			assertTrue(t.getInscripciones().isEmpty());
			assertEquals(1, t.cuposPreferencialesDisponibles(HOY));
		}

		@Test
		@DisplayName("en dobles se retira la pareja completa")
		void retiraPareja() throws ReglaNegocioException {
			Torneo dobles = new TorneoRecreativo("Dobles", new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, true),
					4, INICIO, LIMITE, 15);
			club.registrarTorneo(dobles);
			club.inscribirEnTorneo(dobles, new ParejaTorneo(juancho, nico), HOY);

			club.desinscribirDeTorneo(dobles, nico, HOY);

			assertTrue(dobles.getInscripciones().isEmpty());
			assertEquals(4, dobles.cuposDisponibles());
		}
	}

	@Nested
	@DisplayName("CA-4 torneo en curso")
	class TorneoEnCurso {

		@Test
		@DisplayName("rechaza la salida")
		void rechazaSalida() throws ReglaNegocioException {
			Torneo t = torneo(5);
			inscribir(t, lucho, HOY);
			inscribir(t, juancho, HOY);
			t.iniciar(INICIO);

			assertThrows(ReglaNegocioException.class, () -> club.desinscribirDeTorneo(t, lucho, INICIO));
			assertEquals(2, t.getInscripciones().size());
		}
	}
}
