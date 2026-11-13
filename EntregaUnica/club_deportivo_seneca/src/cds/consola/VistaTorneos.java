package cds.consola;

import java.time.LocalDate;
import java.util.List;

import cds.torneos.EncuentroTorneo;
import cds.torneos.EstadoTorneo;
import cds.torneos.InscripcionTorneo;
import cds.torneos.Posicion;
import cds.torneos.Torneo;

public class VistaTorneos {

	private VistaTorneos() {
	}

	public static String resumen(Torneo t) {
		return t.getNombre() + " | " + t.getTipo() + " | " + t.getEspecialidad().getDescripcion() + " | "
				+ t.getEstado().getNombre() + " | " + t.getInscripciones().size() + "/" + t.getCupos() + " inscritos"
				+ " | inicia " + t.getFechaInicio();
	}

	public static void imprimirLista(List<Torneo> torneos) {
		if (torneos.isEmpty()) {
			System.out.println("  (no hay torneos)");
			return;
		}
		for (Torneo t : torneos) {
			System.out.println("  - " + resumen(t));
		}
	}

	public static void imprimirHistorial(Torneo t, LocalDate hoy) {
		System.out.println();
		System.out.println("===== " + t.getNombre() + " =====");
		System.out.println("Tipo: " + t.getTipo() + " | " + t.getEspecialidad().getDescripcion() + " ("
				+ t.getFormato().getNombre() + ")");
		System.out.println("Estado: " + t.getEstado().getNombre());
		System.out.println("Inicio: " + t.getFechaInicio());
		System.out.println("Premio: " + t.describirPremio());

		System.out.println("\nCupos: " + t.getCupos() + " (" + t.getCuposPreferenciales() + " preferenciales)");
		System.out.println("  Preferenciales ocupados: " + t.contarPreferencialesOcupados() + " | regulares ocupados: "
				+ t.contarRegularesOcupados() + " | libres: " + t.cuposDisponibles());
		if (t.getEstado() == EstadoTorneo.INSCRIPCIONES_ABIERTAS) {
			if (t.preferencialesVigentes(hoy)) {
				System.out.println("  Los preferenciales estan reservados hasta el " + t.getFechaLimitePreferencial());
			} else {
				System.out.println("  Los preferenciales ya se liberaron como regulares");
			}
		}

		System.out.println("\nParticipantes:");
		if (t.getInscripciones().isEmpty()) {
			System.out.println("  (ninguno)");
		}
		for (InscripcionTorneo i : t.getInscripciones()) {
			String extra = i.isPreferencial() ? " [preferencial]" : "";
			if (i.getMontoPagado() > 0) {
				extra += " pago $" + String.format("%,.0f", i.getMontoPagado());
			}
			System.out.println("  - " + i.getParticipante().getNombre() + extra + " (inscrito " + i.getFecha() + ")");
		}

		System.out.println("\nEncuentros:");
		if (t.getEncuentros().isEmpty()) {
			System.out.println("  (sin encuentros)");
		}
		for (EncuentroTorneo e : t.getEncuentros()) {
			System.out.println("  - " + describirEncuentro(e));
		}

		if (!t.getEncuentros().isEmpty()) {
			System.out.println("\nPosiciones:");
			System.out.println(String.format("  %-3s %-28s %3s %3s %3s %3s %4s %4s", "#", "Participante", "PJ", "PG",
					"PE", "PP", "DIF", "PTS"));
			int n = 1;
			for (Posicion p : t.tablaPosiciones()) {
				System.out.println(String.format("  %-3d %-28s %3d %3d %3d %3d %4d %4d", n++,
						recortar(p.getParticipante().getNombre(), 28), p.getJugados(), p.getGanados(),
						p.getEmpatados(), p.getPerdidos(), p.getDiferencia(), p.getPuntos()));
			}
		}

		if (t.getGanador() != null) {
			System.out.println("\nGanador: " + t.getGanador().getParticipante().getNombre());
		}
		System.out.println();
	}

	public static String describirEncuentro(EncuentroTorneo e) {
		String a = e.getParticipanteA().getParticipante().getNombre();
		String b = e.getParticipanteB().getParticipante().getNombre();
		String marcador = e.isJugado() ? e.getMarcadorA() + " - " + e.getMarcadorB() : "pendiente";
		return "[" + e.getRonda() + ", " + e.getFecha() + "] " + a + " vs " + b + ": " + marcador;
	}

	private static String recortar(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max - 1) + ".";
	}
}
