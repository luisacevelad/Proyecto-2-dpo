package cds.torneos;

import cds.ReglaNegocioException;

/**
 * El "que se juega" de un torneo: deporte o disciplina + categoria o nivel.
 *
 * Se modela por composicion (Torneo TIENE una Especialidad) y no por herencia,
 * porque el otro eje (Recreativo / Competitivo) ya usa la herencia. Asi no hay
 * que crear 4 subclases (RecreativoConjunto, CompetitivoIndividual, ...).
 */
public abstract class Especialidad {

	public abstract FormatoTorneo getFormato();

	/** Texto para mostrar, p. ej. "Futbol Sub-15" o "Tenis Intermedio (dobles)". */
	public abstract String getDescripcion();

	/** Si un encuentro de este deporte puede terminar empatado. */
	public abstract boolean permiteEmpate();

	/**
	 * Revisa que el participante corresponda a este deporte y categoria.
	 * Lanza excepcion con el motivo si no corresponde.
	 */
	public abstract void validar(Participante p) throws ReglaNegocioException;
}
