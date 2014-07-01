package br.ifes.leds.sincap.web.model;

import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.ProcessoNotificacao;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author 20102bsi0553
 */
@Getter
@Setter
public class UsuarioSessao implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long idUsuario;
    private Long idHospital;
    private String cpfUsuario;
    private ProcessoNotificacao notificacao;
    private int etapaNotifica;
    private String nome;
}