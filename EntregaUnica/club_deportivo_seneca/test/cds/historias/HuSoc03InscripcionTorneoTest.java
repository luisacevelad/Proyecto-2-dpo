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
import cds.torneos.Inscripcion;
import cds.torneos.JugadorClub;
import cds.torneos.Torneo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Socio;

@DisplayName("HU-SOC-03 Inscribirse o salirse de un torneo")
class HuSoc03InscripcionTorneoTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 11);
	private static final LocalDate INICIO = LocalDate.of(2026, 11, 21);

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
		Torneo t = new TorneoRecreativo("Copa de tenis", Disciplina.TENIS, Nivel.INTERMEDIO, false, cupos, INICIO);
		club.registrarTorneo(t);
		return t;
	}

	private Inscripcion inscribir(Torneo t, Socio s, LocalDate fecha) throws ReglaNegocioException {
		return club.inscribirEnTorneo(t, List.of(new JugadorClub(s)), fecha);
	}

	@Nested
	@DisplayName("CA-1 socio en el top del ranking")
	class SocioTop {

		@Test
		@DisplayName("ocupa un cupo preferencial")
		void ocupaPreferencial() throws ReglaNegocioException {
			Torneo t = torneo(5);

			assertTrue(inscribir(t, lucho, HOY).isCupoPreferencial());
			assertFalse(inscribir(t, juancho, HOY).isCupoPreferencial());
		}
	}

	@Nested
	@DisplayName("CA-2 solo quedan cupos preferenciales")
	class SoloPreferenciales {

		@Test
		@DisplayName("rechaza a un socio fuera del top si faltan mas de 3 dias")
		void rechazaFueraDelTop() throws ReglaNegocioException {
			Torneo t = torneo(2);
			inscribir(t, juancho, HOY);

			assertThrows(ReglaNegocioException.class, () -> inscribir(t, nico, HOY));
			assertEquals(1, t.getInscripciones().size());
		}

		@Test
		@DisplayName("a 3 dias del inicio los cupos preferenciales se liberan")
		void liberaPreferenciales() throws ReglaNegocioException {
			Torneo t = torneo(2);
			inscribir(t, juancho, HOY);

			Inscripcion i = inscribir(t, nico, INICIO.minusDays(3));

			assertEquals(2, t.getInscripciones().size());
			assertTrue(i.isCupoPreferencial());
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

			t.retirar(new JugadorClub(lucho));

			assertTrue(t.getInscripciones().isEmpty());
			assertEquals(1, t.preferencialesLibres());
		}

		@Test
		@DisplayName("en dobles se retira la pareja completa")
		void retiraPareja() throws ReglaNegocioException {
			Torneo dobles = new TorneoRecreativo("Dobles", Disciplina.TENIS, Nivel.INTERMEDIO, true, 4, INICIO);
			club.registrarTorneo(dobles);
			club.inscribirEnTorneo(dobles, List.of(new JugadorClub(juancho), new JugadorClub(nico)), HOY);

			dobles.retirar(new JugadorClub(nico));

			assertTrue(dobles.getInscripciones().isEmpty());
			assertEquals(3, dobles.regularesLibres());
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

			assertThrows(ReglaNegocioException.class, () -> t.retirar(new JugadorClub(lucho)));
			assertEquals(2, t.getInscripciones().size());
		}
	}
}
