package cds.torneos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cds.Club;
import cds.academias.Academia;
import cds.academias.InscripcionAcademia;
import cds.competencias.Competencia;
import cds.competencias.Liga;
import cds.conjunto.Equipo;
import cds.usuarios.Usuario;


public class RankingTorneos {
	private final Club club;

	public RankingTorneos(Club club) {
		this.club = club;
	}

	public boolean esPreferencial(Participante p, Torneo t) {
		double suPuntaje = puntaje(p, t.getEspecialidad());
		if (suPuntaje <= 0) {
			return false;
		}
		int k = t.getCuposPreferenciales();
		if (t.getFormato() == FormatoTorneo.DOBLES) {
			k = k * 2;
		}
		List<Double> puntajes = puntajesCategoria(t.getEspecialidad());
		if (puntajes.isEmpty()) {
			return false;
		}
		Collections.sort(puntajes, Collections.reverseOrder());
		double umbral = puntajes.get(Math.min(k, puntajes.size()) - 1);
		return suPuntaje >= umbral;
	}

	public double puntaje(Participante p, Especialidad esp) {
		if (p instanceof EquipoTorneo) {
			return desempenoEquipo(((EquipoTorneo) p).getEquipo());
		}
		if (!(esp instanceof EspecialidadIndividual)) {
			return 0;
		}
		EspecialidadIndividual ind = (EspecialidadIndividual) esp;
		double suma = 0;
		for (Usuario u : p.getIntegrantes()) {
			suma += rankingJugador(u, ind);
		}
		return suma / p.getIntegrantes().size();
	}

	private List<Double> puntajesCategoria(Especialidad esp) {
		List<Double> puntajes = new ArrayList<Double>();
		if (esp instanceof EspecialidadConjunto) {
			EspecialidadConjunto c = (EspecialidadConjunto) esp;
			for (Equipo e : club.getEquipos()) {
				if (e.isActivo() && e.getDeporte() == c.getDeporte() && e.getCategoria() == c.getCategoria()) {
					double d = desempenoEquipo(e);
					if (d > 0) {
						puntajes.add(d);
					}
				}
			}
		} else {
			EspecialidadIndividual ind = (EspecialidadIndividual) esp;
			for (Academia a : club.getAcademias()) {
				if (a.getDisciplina() == ind.getDisciplina()) {
					for (InscripcionAcademia i : a.getInscripciones()) {
						if (i.getNivel() == ind.getNivel()) {
							puntajes.add(i.getPuntajeRanking());
						}
					}
				}
			}
		}
		return puntajes;
	}

	private double rankingJugador(Usuario u, EspecialidadIndividual ind) {
		for (Academia a : club.getAcademias()) {
			if (a.getDisciplina() == ind.getDisciplina()) {
				for (InscripcionAcademia i : a.getInscripciones()) {
					if (i.getSocio() == u && i.getNivel() == ind.getNivel()) {
						return i.getPuntajeRanking();
					}
				}
			}
		}
		return 0;
	}

	public double desempenoEquipo(Equipo e) {
		double puntos = 0;
		for (Competencia c : club.getCompetencias()) {
			if (c instanceof Liga) {
				puntos += ((Liga) c).getPuntos(e);
			}
		}
		return puntos;
	}
}
