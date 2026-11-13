package cds.torneos;

import cds.ReglaNegocioException;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;

/**
 * Torneo de deporte de conjunto, p. ej. mini-campeonato de futbol Sub-15.
 * Reutiliza DeporteConjunto y CategoriaEdad del Proyecto 1.
 */
public class EspecialidadConjunto extends Especialidad {
	private final DeporteConjunto deporte;
	private final CategoriaEdad categoria;

	public EspecialidadConjunto(DeporteConjunto deporte, CategoriaEdad categoria) {
		this.deporte = deporte;
		this.categoria = categoria;
	}

	@Override
	public FormatoTorneo getFormato() {
		return FormatoTorneo.CONJUNTO;
	}

	@Override
	public String getDescripcion() {
		return deporte.getNombre() + " " + categoria.getNombre();
	}

	@Override
	public boolean permiteEmpate() {
		return deporte.permiteEmpate();
	}

	@Override
	public void validar(Participante p) throws ReglaNegocioException {
		if (!(p instanceof EquipoTorneo)) {
			throw new ReglaNegocioException("En " + getDescripcion() + " solo se inscriben equipos completos");
		}
		Equipo e = ((EquipoTorneo) p).getEquipo();
		if (!e.isActivo()) {
			throw new ReglaNegocioException(e.getNombre() + " no esta activo");
		}
		if (e.getDeporte() != deporte || e.getCategoria() != categoria) {
			throw new ReglaNegocioException(e.getNombre() + " no es de " + getDescripcion());
		}
		int habilitados = e.contarHabilitados();
		if (habilitados < deporte.getMinJugadores()) {
			throw new ReglaNegocioException(e.getNombre() + " no esta completo: tiene " + habilitados
					+ " jugadores habilitados y " + deporte.getNombre() + " exige " + deporte.getMinJugadores());
		}
	}

	public DeporteConjunto getDeporte() {
		return deporte;
	}

	public CategoriaEdad getCategoria() {
		return categoria;
	}
}
