package cds.consola;

import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.conjunto.Equipo;
import cds.torneos.EquipoTorneo;
import cds.torneos.EspecialidadConjunto;
import cds.torneos.EstadoTorneo;
import cds.torneos.FormatoTorneo;
import cds.torneos.InscripcionTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.ParejaTorneo;
import cds.torneos.Participante;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.usuarios.Administrador;
import cds.usuarios.Usuario;

/**
 * Opciones de torneos para quien juega: socios y empleados. Las reglas
 * (cupos, ranking, quien puede inscribirse) las valida el modelo en
 * Club.inscribirEnTorneo; la consola solo arma el participante y muestra el resultado.
 */
public class MenuTorneosJugador {
	private final ConsolaBase consola;
	private final Entrada entrada;

	public MenuTorneosJugador(ConsolaBase consola) {
		this.consola = consola;
		this.entrada = consola.entrada;
	}

	public Menu construir() {
		return new Menu("Torneos", entrada)
				.agregar("Ver torneos con inscripciones abiertas", this::verAbiertos)
				.agregar("Inscribirme en un torneo", this::inscribirme)
				.agregar("Salirme de un torneo", this::salirme)
				.agregar("Ver mis torneos", this::verMisTorneos)
				.agregar("Ver detalle e historial de un torneo", this::verDetalle);
	}

	private List<Torneo> abiertos() {
		List<Torneo> resultado = new ArrayList<Torneo>();
		for (Torneo t : consola.club.torneosPorEstado(EstadoTorneo.INSCRIPCIONES_ABIERTAS)) {
			if (t.inscripcionesAbiertas(consola.hoy())) {
				resultado.add(t);
			}
		}
		return resultado;
	}

	private List<Torneo> misTorneos() {
		List<Torneo> resultado = new ArrayList<Torneo>();
		for (Torneo t : consola.club.getTorneos()) {
			if (t.buscarInscripcion(consola.usuario) != null) {
				resultado.add(t);
			}
		}
		return resultado;
	}

	private Torneo elegir(List<Torneo> torneos) throws ReglaNegocioException {
		return entrada.elegir("Torneos", torneos, VistaTorneos::resumen);
	}

	private void verAbiertos() {
		List<Torneo> torneos = abiertos();
		if (torneos.isEmpty()) {
			System.out.println("No hay torneos con inscripciones abiertas.");
			return;
		}
		for (Torneo t : torneos) {
			System.out.println("- " + VistaTorneos.resumen(t));
			System.out.println("    Cupos libres: " + t.cuposDisponibles() + " (regulares: " + t.cuposRegularesDisponibles(consola.hoy())
					+ ", preferenciales: " + t.cuposPreferencialesDisponibles(consola.hoy()) + ") | " + costo(t)
					+ " | Premio: " + t.describirPremio());
		}
	}

	private static String costo(Torneo t) {
		if (t instanceof TorneoCompetitivo) {
			return "Inscripcion: " + ConsolaBase.pesos(((TorneoCompetitivo) t).getTarifa());
		}
		return "Inscripcion gratis";
	}

	private void inscribirme() throws ReglaNegocioException {
		Torneo torneo = elegir(abiertos());
		Participante participante = armarParticipante(torneo);
		if (torneo instanceof TorneoCompetitivo && !entrada.siNo(costo(torneo) + ". Confirmar inscripcion?")) {
			System.out.println("Inscripcion cancelada.");
			return;
		}
		InscripcionTorneo inscripcion = consola.club.inscribirEnTorneo(torneo, participante, consola.hoy());
		System.out.println(participante.getNombre() + " quedo inscrito en " + torneo.getNombre()
				+ (inscripcion.isPreferencial() ? " con cupo PREFERENCIAL (por ranking)" : " con cupo regular")
				+ ". Pago: " + ConsolaBase.pesos(inscripcion.getMontoPagado()));
	}

	/** Arma el participante segun el formato del torneo: el jugador, su pareja o su equipo. */
	private Participante armarParticipante(Torneo torneo) throws ReglaNegocioException {
		if (torneo.getFormato() == FormatoTorneo.INDIVIDUAL) {
			return new JugadorTorneo(consola.usuario);
		}
		if (torneo.getFormato() == FormatoTorneo.DOBLES) {
			List<Usuario> companeros = new ArrayList<Usuario>();
			for (Usuario u : consola.club.getUsuarios()) {
				if (u != consola.usuario && !(u instanceof Administrador)) {
					companeros.add(u);
				}
			}
			Usuario pareja = entrada.elegir("Companero(a) de dobles", companeros, u -> u.getNombre() + " (" + u.getLogin() + ")");
			return new ParejaTorneo(consola.usuario, pareja);
		}
		EspecialidadConjunto especialidad = (EspecialidadConjunto) torneo.getEspecialidad();
		List<Equipo> misEquipos = new ArrayList<Equipo>();
		for (Equipo e : consola.club.getEquipos()) {
			if (e.getDeporte() == especialidad.getDeporte() && e.getCategoria() == especialidad.getCategoria()
					&& e.getMiembros().stream().anyMatch(m -> m.getSocio() == consola.usuario)) {
				misEquipos.add(e);
			}
		}
		if (misEquipos.isEmpty()) {
			throw new ReglaNegocioException("No pertenece a ningun equipo de " + especialidad.getDescripcion());
		}
		return new EquipoTorneo(consola.elegirEquipo(misEquipos));
	}

	private void salirme() throws ReglaNegocioException {
		List<Torneo> torneos = new ArrayList<Torneo>();
		for (Torneo t : misTorneos()) {
			if (t.getEstado() == EstadoTorneo.INSCRIPCIONES_ABIERTAS) {
				torneos.add(t);
			}
		}
		Torneo torneo = elegir(torneos);
		String nombre = torneo.buscarInscripcion(consola.usuario).getParticipante().getNombre();
		if (!entrada.siNo("Sacar a " + nombre + " de " + torneo.getNombre() + "? Se libera el cupo")) {
			return;
		}
		consola.club.desinscribirDeTorneo(torneo, consola.usuario, consola.hoy());
		System.out.println(nombre + " ya no esta inscrito en " + torneo.getNombre() + ". El cupo quedo libre.");
	}

	private void verMisTorneos() {
		List<Torneo> torneos = misTorneos();
		if (torneos.isEmpty()) {
			System.out.println("No esta inscrito en ningun torneo.");
			return;
		}
		for (Torneo t : torneos) {
			InscripcionTorneo i = t.buscarInscripcion(consola.usuario);
			String extra = "";
			if (t.getGanador() == i) {
				extra = " - GANADOR: " + t.describirPremio();
			}
			System.out.println("- " + VistaTorneos.resumen(t) + " | como " + i.getParticipante().getNombre()
					+ (i.isPreferencial() ? " (preferencial)" : "") + extra);
		}
	}

	private void verDetalle() throws ReglaNegocioException {
		VistaTorneos.imprimirHistorial(elegir(consola.club.getTorneos()), consola.hoy());
	}
}
