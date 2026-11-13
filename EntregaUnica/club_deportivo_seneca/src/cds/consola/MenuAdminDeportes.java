package cds.consola;

import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Disciplina;
import cds.academias.Instalacion;
import cds.academias.Nivel;
import cds.academias.TipoInstalacion;
import cds.competencias.Competencia;
import cds.competencias.CompetenciaPorFase;
import cds.competencias.Liga;
import cds.competencias.TipoFase;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.conjunto.MiembroEquipo;
import cds.partidos.Partido;
import cds.usuarios.Socio;

/** Administracion de equipos, academias, instalaciones y competencias. */
public class MenuAdminDeportes {
	private final ConsolaAdministrador consola;
	private final Entrada entrada;

	public MenuAdminDeportes(ConsolaAdministrador consola) {
		this.consola = consola;
		this.entrada = consola.entrada;
	}

	public Menu construir() {
		return new Menu("Equipos, academias y competencias", entrada)
				.agregar("Crear equipo", this::crearEquipo)
				.agregar("Ver equipos y jugadores", this::verEquipos)
				.agregar("Crear academia", this::crearAcademia)
				.agregar("Agregar instalacion a una academia", this::agregarInstalacion)
				.agregar("Inscribir socio en una academia", this::inscribirEnAcademia)
				.agregar("Crear competencia", this::crearCompetencia)
				.agregar("Agregar fase a una competencia por fases", this::agregarFase)
				.agregar("Reporte de partidos jugados", this::reportePartidos);
	}

	private void crearEquipo() throws ReglaNegocioException {
		String nombre = entrada.texto("Nombre del equipo");
		DeporteConjunto deporte = entrada.elegirEnum("Deporte", DeporteConjunto.values());
		CategoriaEdad categoria = entrada.elegirEnum("Categoria", CategoriaEdad.values());
		int cupo = entrada.entero("Cupo maximo de jugadores", deporte.getMinJugadores(), 40);
		consola.club.registrarEquipo(new Equipo(nombre, deporte, categoria, cupo));
		System.out.println("Equipo " + nombre + " creado.");
	}

	private void verEquipos() {
		if (consola.club.getEquipos().isEmpty()) {
			System.out.println("No hay equipos.");
		}
		for (Equipo e : consola.club.getEquipos()) {
			System.out.println(e.getNombre() + " - " + e.getDeporte().getNombre() + " " + e.getCategoria().getNombre()
					+ " (" + e.getMiembros().size() + "/" + e.getCupoMaximo() + ", " + e.contarHabilitados() + " habilitados)");
			for (MiembroEquipo m : e.getMiembros()) {
				System.out.println("   #" + m.getNumeroCamiseta() + " " + m.getSocio().getNombre() + " - " + m.getPosicion()
						+ (e.estaSuspendido(m.getSocio()) ? " (suspendido)" : ""));
			}
		}
	}

	private void crearAcademia() throws ReglaNegocioException {
		String nombre = entrada.texto("Nombre de la academia");
		Disciplina disciplina = entrada.elegirEnum("Disciplina", Disciplina.values());
		int maximo = entrada.entero("Maximo de instalaciones simultaneas", 1, 20);
		consola.club.registrarAcademia(new Academia(nombre, disciplina, maximo));
		System.out.println("Academia " + nombre + " creada.");
	}

	private void agregarInstalacion() throws ReglaNegocioException {
		Academia academia = consola.elegirAcademia(consola.club.getAcademias());
		String nombre = entrada.texto("Nombre de la instalacion");
		TipoInstalacion tipo = entrada.elegirEnum("Tipo", TipoInstalacion.values());
		boolean techada = entrada.siNo("Es techada?");
		consola.club.agregarInstalacion(academia, new Instalacion(nombre, tipo, techada));
		System.out.println("Instalacion " + nombre + " agregada a " + academia.getNombre() + ".");
	}

	private void inscribirEnAcademia() throws ReglaNegocioException {
		Academia academia = consola.elegirAcademia(consola.club.getAcademias());
		Socio s = consola.elegirSocio(consola.socios());
		Nivel nivel = entrada.elegirEnum("Nivel", Nivel.values());
		academia.inscribirSocio(s, nivel, consola.hoy());
		System.out.println(s.getNombre() + " inscrito en " + academia.getNombre() + " (" + nivel + ").");
	}

	private void crearCompetencia() throws ReglaNegocioException {
		boolean esLiga = entrada.siNo("Es una liga? (n = competencia por fases)");
		String nombre = entrada.texto("Nombre");
		String organizador = entrada.texto("Organizador");
		int temporada = entrada.entero("Temporada", 2000, 2100);
		DeporteConjunto deporte = entrada.elegirEnum("Deporte", DeporteConjunto.values());
		CategoriaEdad categoria = entrada.elegirEnum("Categoria", CategoriaEdad.values());
		int nominaMinima = entrada.entero("Nomina minima", deporte.getMinJugadores(), 40);
		Competencia competencia;
		if (esLiga) {
			int victoria = entrada.entero("Puntos por victoria", 1, 10);
			int empate = deporte.permiteEmpate() ? entrada.entero("Puntos por empate", 0, victoria) : 0;
			competencia = new Liga(nombre, organizador, temporada, deporte, categoria, nominaMinima, victoria, empate);
		} else {
			competencia = new CompetenciaPorFase(nombre, organizador, temporada, deporte, categoria, nominaMinima);
		}
		consola.club.registrarCompetencia(competencia);
		System.out.println("Competencia " + nombre + " creada.");
	}

	private void agregarFase() throws ReglaNegocioException {
		List<CompetenciaPorFase> porFases = new ArrayList<CompetenciaPorFase>();
		for (Competencia c : consola.club.getCompetencias()) {
			if (c instanceof CompetenciaPorFase) {
				porFases.add((CompetenciaPorFase) c);
			}
		}
		CompetenciaPorFase competencia = entrada.elegir("Competencias por fases", porFases, Competencia::getNombre);
		String nombre = entrada.texto("Nombre de la fase");
		TipoFase tipo = entrada.elegirEnum("Tipo de fase", TipoFase.values());
		competencia.agregarFase(nombre, competencia.getFases().size() + 1, tipo);
		System.out.println("Fase " + nombre + " agregada.");
	}

	private void reportePartidos() {
		if (consola.club.partidosJugados().isEmpty()) {
			System.out.println("No hay partidos jugados.");
		}
		for (Partido p : consola.club.partidosJugados()) {
			System.out.println("- " + p.getFecha() + " " + p.getClass().getSimpleName() + ": " + p.getMarcadorLocal() + " - "
					+ p.getMarcadorVisitante() + " (" + p.getParticipantes().size() + " jugadores)");
		}
	}
}
