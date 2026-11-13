package cds.torneos;

import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;

/**
 * Torneo de disciplina individual, p. ej. cuadro de tenis nivel Intermedio.
 * Puede ser de sencillos (se inscriben jugadores) o de dobles (parejas).
 * Reutiliza Disciplina y Nivel de las academias del Proyecto 1.
 */
public class EspecialidadIndividual extends Especialidad {
	private final Disciplina disciplina;
	private final Nivel nivel;
	private final boolean dobles;

	public EspecialidadIndividual(Disciplina disciplina, Nivel nivel, boolean dobles) {
		this.disciplina = disciplina;
		this.nivel = nivel;
		this.dobles = dobles;
	}

	@Override
	public FormatoTorneo getFormato() {
		if (dobles) {
			return FormatoTorneo.DOBLES;
		}
		return FormatoTorneo.INDIVIDUAL;
	}

	@Override
	public String getDescripcion() {
		String texto = disciplina + " " + nivel;
		if (dobles) {
			texto += " (dobles)";
		}
		return texto;
	}

	@Override
	public boolean permiteEmpate() {
		// En tenis, padel y tenis de mesa siempre hay un ganador
		return false;
	}

	@Override
	public void validar(Participante p) throws ReglaNegocioException {
		if (p.getFormato() != getFormato()) {
			if (dobles) {
				throw new ReglaNegocioException(getDescripcion() + " es de dobles: se inscriben parejas");
			}
			throw new ReglaNegocioException(getDescripcion() + " es de sencillos: se inscriben jugadores");
		}
	}

	public Disciplina getDisciplina() {
		return disciplina;
	}

	public Nivel getNivel() {
		return nivel;
	}

	public boolean isDobles() {
		return dobles;
	}
}
