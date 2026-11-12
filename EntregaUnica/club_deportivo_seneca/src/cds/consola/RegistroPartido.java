package cds.consola;

import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.conjunto.DeporteConjunto;
import cds.partidos.EstadisticaBaloncesto;
import cds.partidos.EstadisticaFutbol;
import cds.partidos.EstadisticaIndividual;
import cds.partidos.EstadisticaRaqueta;
import cds.partidos.EstadisticaTenisMesa;
import cds.partidos.EstadisticaVoleibol;
import cds.partidos.Partido;
import cds.usuarios.Socio;

/**
 * Pasos comunes para registrar cualquier partido por consola:
 * participantes, estadisticas y marcador final.
 */
public class RegistroPartido {
	private final Entrada entrada;

	public RegistroPartido(Entrada entrada) {
		this.entrada = entrada;
	}

	/** Agrega participantes uno a uno; si el modelo rechaza a alguno, se informa y se sigue. */
	public void agregarParticipantes(Partido partido, List<Socio> candidatos, int maximo) throws ReglaNegocioException {
		while (partido.getParticipantes().size() < maximo) {
			List<Socio> libres = new ArrayList<Socio>(candidatos);
			libres.removeAll(partido.getParticipantes());
			if (libres.isEmpty()) {
				return;
			}
			Socio s = entrada.elegir("Jugador " + (partido.getParticipantes().size() + 1), libres,
					j -> j.getNombre() + " (" + j.getLogin() + ")");
			try {
				partido.agregarParticipante(s);
				System.out.println("  " + s.getNombre() + " agregado.");
			} catch (ReglaNegocioException e) {
				System.out.println("  Rechazado: " + e.getMessage());
			}
			if (partido.getParticipantes().size() < maximo && !entrada.siNo("Agregar otro jugador?")) {
				return;
			}
		}
	}

	/** Arma una lista de jugadores elegidos uno a uno, sin validarlos todavia. */
	public List<Socio> elegirVarios(List<Socio> candidatos) throws ReglaNegocioException {
		List<Socio> elegidos = new ArrayList<Socio>();
		do {
			List<Socio> libres = new ArrayList<Socio>(candidatos);
			libres.removeAll(elegidos);
			if (libres.isEmpty()) {
				break;
			}
			elegidos.add(entrada.elegir("Jugador " + (elegidos.size() + 1), libres, j -> j.getNombre() + " (" + j.getLogin() + ")"));
		} while (entrada.siNo("Agregar otro jugador?"));
		return elegidos;
	}

	public void estadisticasConjunto(Partido partido, DeporteConjunto deporte) throws ReglaNegocioException {
		for (Socio s : partido.getParticipantes()) {
			if (entrada.siNo("Registrar estadisticas de " + s.getNombre() + "?")) {
				partido.registrarEstadistica(crearConjunto(deporte, s));
			}
		}
	}

	public void estadisticasIndividuales(Partido partido, Disciplina disciplina) throws ReglaNegocioException {
		for (Socio s : partido.getParticipantes()) {
			if (entrada.siNo("Registrar estadisticas de " + s.getNombre() + "?")) {
				partido.registrarEstadistica(crearIndividual(disciplina, s));
			}
		}
	}

	private EstadisticaIndividual crearConjunto(DeporteConjunto deporte, Socio s) {
		switch (deporte) {
		case FUTBOL:
			return new EstadisticaFutbol(s, entrada.entero("Goles", 0, 50), entrada.entero("Asistencias", 0, 50),
					entrada.entero("Tarjetas amarillas", 0, 2), entrada.entero("Tarjetas rojas", 0, 1));
		case BALONCESTO:
			return new EstadisticaBaloncesto(s, entrada.entero("Puntos", 0, 200), entrada.entero("Rebotes", 0, 100));
		default:
			return new EstadisticaVoleibol(s, entrada.entero("Aces", 0, 100), entrada.entero("Bloqueos", 0, 100));
		}
	}

	private EstadisticaIndividual crearIndividual(Disciplina disciplina, Socio s) {
		if (disciplina == Disciplina.TENIS_MESA) {
			return new EstadisticaTenisMesa(s, entrada.entero("Sets ganados", 0, 7), entrada.entero("Puntos totales", 0, 300));
		}
		return new EstadisticaRaqueta(s, entrada.entero("Games ganados", 0, 100), entrada.entero("Sets ganados", 0, 5),
				entrada.entero("Aces", 0, 100), entrada.entero("Errores no forzados", 0, 200));
	}

	public void finalizar(Partido partido, String nombreLocal, String nombreVisitante) throws ReglaNegocioException {
		int local = entrada.entero("Marcador de " + nombreLocal, 0, 300);
		int visitante = entrada.entero("Marcador de " + nombreVisitante, 0, 300);
		partido.finalizar(local, visitante);
		System.out.println("Partido finalizado: " + nombreLocal + " " + local + " - " + visitante + " " + nombreVisitante);
	}
}
