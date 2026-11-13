package cds.torneos;

import cds.ReglaNegocioException;

public abstract class Especialidad {

	public abstract FormatoTorneo getFormato();

	public abstract String getDescripcion();

	public abstract boolean permiteEmpate();

	public abstract void validar(Participante p) throws ReglaNegocioException;
}
