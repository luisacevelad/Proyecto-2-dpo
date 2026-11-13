package cds.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cds.Club;
import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.catalogo.Articulo;
import cds.torneos.EncuentroTorneo;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.EstadoTorneo;
import cds.torneos.InscripcionTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.ParejaTorneo;
import cds.torneos.Posicion;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Entrenador;
import cds.usuarios.Socio;
import cds.ventas.VentaTienda;

/**
 * Persistencia: al guardar y volver a cargar club.json no se pierde informacion,
 * incluidos los torneos del Proyecto 2.
 */
@DisplayName("Persistencia en club.json")
class ArchivoClubTest {
	private Path carpeta;
	private Club club;

	@BeforeEach
	void setUp() throws IOException, ReglaNegocioException {
		carpeta = Files.createTempDirectory("cds-prueba");
		club = clubDePrueba();
	}

	@AfterEach
	void limpiar() throws IOException {
		try (Stream<Path> archivos = Files.walk(carpeta)) {
			archivos.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		}
	}

	/** Club pequeno con usuarios, una venta, una mensualidad y dos torneos en estados distintos. */
	private static Club clubDePrueba() throws ReglaNegocioException {
		Club c = new Club();
		Socio ana = new Socio("ana", "ana123", "Ana", "D1", LocalDate.of(2000, 1, 1));
		Socio beto = new Socio("beto", "beto123", "Beto", "D2", LocalDate.of(1999, 1, 1));
		Socio carla = new Socio("carla", "carla123", "Carla", "D3", LocalDate.of(2001, 1, 1));
		Socio dani = new Socio("dani", "dani123", "Dani", "D4", LocalDate.of(2002, 1, 1));
		Entrenador leo = new Entrenador("leo", "leo123", "Leo", "LEO-CDS");
		for (cds.usuarios.Usuario u : List.of(ana, beto, carla, dani, leo)) {
			c.registrarUsuario(u);
		}

		Articulo balon = new Articulo("Balon", "Implementos", 100000);
		c.getTienda().registrarArticulo(balon);
		VentaTienda venta = new VentaTienda(LocalDate.of(2026, 11, 5), ana);
		venta.agregarLinea(balon, 1);
		venta.aplicarCodigo(leo);
		venta.confirmar();
		c.getTienda().registrarVenta(venta);
		c.generarMensualidad(ana, YearMonth.of(2026, 11), 150000);

		// Torneo recreativo individual, finalizado
		Torneo tenis = new TorneoRecreativo("Liga de Tenis", new EspecialidadIndividual(Disciplina.TENIS, Nivel.INTERMEDIO, false),
				4, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 4), 15);
		c.registrarTorneo(tenis);
		c.inscribirEnTorneo(tenis, new JugadorTorneo(ana), LocalDate.of(2026, 11, 1));
		c.inscribirEnTorneo(tenis, new JugadorTorneo(beto), LocalDate.of(2026, 11, 2));
		tenis.iniciar(LocalDate.of(2026, 11, 9));
		EncuentroTorneo final1 = tenis.programarEncuentro(tenis.getInscripciones().get(0), tenis.getInscripciones().get(1),
				LocalDate.of(2026, 11, 10), "Final");
		tenis.registrarResultado(final1, 2, 1);
		tenis.finalizar();

		// Torneo competitivo de dobles, inscripciones abiertas
		Torneo padel = new TorneoCompetitivo("Copa de Padel", new EspecialidadIndividual(Disciplina.PADEL, Nivel.INTERMEDIO, true),
				6, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 3), 40000);
		c.registrarTorneo(padel);
		c.inscribirEnTorneo(padel, new ParejaTorneo(carla, dani), LocalDate.of(2026, 11, 12));
		padel.liberarCuposPreferenciales();
		return c;
	}

	private Club guardarYCargar(Club c) throws IOException, ReglaNegocioException {
		ArchivoClub.guardar(c, carpeta.toString());
		return ArchivoClub.cargar(carpeta.toString());
	}

	private String contenido() throws IOException {
		return new String(Files.readAllBytes(carpeta.resolve("club.json")), StandardCharsets.UTF_8);
	}

	@Nested
	@DisplayName("Ida y vuelta")
	class IdaYVuelta {

		@Test
		@DisplayName("conserva usuarios, puntos, ventas y mensualidades")
		void datosDelProyecto1() throws IOException, ReglaNegocioException {
			Club cargado = guardarYCargar(club);
			assertEquals(5, cargado.getUsuarios().size());
			assertEquals(club.buscarUsuario("ana").getPuntosFidelidad(), cargado.buscarUsuario("ana").getPuntosFidelidad());
			assertEquals(1, cargado.getTienda().getVentas().size());
			assertEquals(club.getTienda().getVentas().get(0).total(), cargado.getTienda().getVentas().get(0).total(), 0.001);
			assertEquals(1, cargado.getTienda().getMensualidades().size());
			assertNotNull(cargado.autenticar("leo", "leo123"));
		}

		@Test
		@DisplayName("conserva un torneo finalizado con encuentros, tabla y ganador")
		void torneoFinalizado() throws IOException, ReglaNegocioException {
			Club cargado = guardarYCargar(club);
			Torneo t = cargado.buscarTorneo("Liga de Tenis");
			assertNotNull(t);
			assertTrue(t instanceof TorneoRecreativo);
			assertEquals(15, ((TorneoRecreativo) t).getPorcentajeBono(), 0.001);
			assertEquals(EstadoTorneo.FINALIZADO, t.getEstado());
			assertEquals(2, t.getInscripciones().size());
			EncuentroTorneo e = t.getEncuentros().get(0);
			assertTrue(e.isJugado());
			assertEquals("Final", e.getRonda());
			assertEquals(2, e.getMarcadorA());
			assertEquals("Ana", t.getGanador().getParticipante().getNombre());
			List<Posicion> tabla = t.tablaPosiciones();
			assertEquals("Ana", tabla.get(0).getParticipante().getNombre());
		}

		@Test
		@DisplayName("conserva parejas, tarifa, montos pagados y cupos liberados")
		void torneoAbiertoDeDobles() throws IOException, ReglaNegocioException {
			Club cargado = guardarYCargar(club);
			Torneo t = cargado.buscarTorneo("Copa de Padel");
			assertTrue(t instanceof TorneoCompetitivo);
			assertEquals(40000, ((TorneoCompetitivo) t).getTarifa(), 0.001);
			assertEquals(EstadoTorneo.INSCRIPCIONES_ABIERTAS, t.getEstado());
			assertTrue(t.isPreferencialesLiberados());
			InscripcionTorneo i = t.getInscripciones().get(0);
			assertTrue(i.getParticipante() instanceof ParejaTorneo);
			assertEquals(40000, i.getMontoPagado(), 0.001);
			assertNotNull(t.buscarInscripcion(cargado.buscarUsuario("dani")));
			assertNull(t.getGanador());
		}

		@Test
		@DisplayName("lo cargado sigue funcionando: se puede inscribir y desinscribir")
		void sigueFuncionando() throws IOException, ReglaNegocioException {
			Club cargado = guardarYCargar(club);
			Torneo t = cargado.buscarTorneo("Copa de Padel");
			cargado.inscribirEnTorneo(t, new ParejaTorneo(cargado.buscarUsuario("ana"), cargado.buscarUsuario("beto")),
					LocalDate.of(2026, 11, 12));
			assertEquals(2, t.getInscripciones().size());
			cargado.desinscribirDeTorneo(t, cargado.buscarUsuario("carla"), LocalDate.of(2026, 11, 12));
			assertEquals(1, t.getInscripciones().size());
		}

		@Test
		@DisplayName("guardar lo cargado produce exactamente el mismo archivo")
		void idempotente() throws IOException, ReglaNegocioException {
			ArchivoClub.guardar(club, carpeta.toString());
			String primero = contenido();
			ArchivoClub.guardar(ArchivoClub.cargar(carpeta.toString()), carpeta.toString());
			assertEquals(primero, contenido());
		}
	}

	@Nested
	@DisplayName("Archivos invalidos o antiguos")
	class Errores {

		@Test
		@DisplayName("un archivo sin la seccion de torneos (Proyecto 1) carga sin torneos")
		void archivoAntiguo() throws IOException, ReglaNegocioException {
			ArchivoClub.guardar(club, carpeta.toString());
			String sinTorneos = contenido().replaceFirst("(?s),\\s*\"torneos\"\\s*:\\s*\\[.*\\]\\s*\\}\\s*$", "\n}\n");
			Files.write(carpeta.resolve("club.json"), sinTorneos.getBytes(StandardCharsets.UTF_8));
			Club cargado = ArchivoClub.cargar(carpeta.toString());
			assertTrue(cargado.getTorneos().isEmpty());
			assertEquals(5, cargado.getUsuarios().size());
		}

		@Test
		@DisplayName("si no existe el archivo se informa con IOException")
		void sinArchivo() {
			assertThrows(IOException.class, () -> ArchivoClub.cargar(carpeta.resolve("no-existe").toString()));
		}

		@Test
		@DisplayName("un torneo que menciona un usuario inexistente no se carga")
		void referenciaRota() throws IOException {
			ArchivoClub.guardar(club, carpeta.toString());
			String roto = contenido().replace("\"login\": \"ana\"", "\"login\": \"fantasma\"");
			Files.write(carpeta.resolve("club.json"), roto.getBytes(StandardCharsets.UTF_8));
			assertThrows(IOException.class, () -> ArchivoClub.cargar(carpeta.toString()));
		}
	}
}
