package br.ifes.leds.sincap.web.test;

import br.ifes.leds.reuse.endereco.cdp.dto.EnderecoDTO;
import br.ifes.leds.sincap.controleInterno.cln.cgt.AplCadastroInterno;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto.ObitoDTO;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto.PacienteDTO;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cdp.dto.ProcessoNotificacaoDTO;
import br.ifes.leds.sincap.gerenciaNotificacao.cln.cgt.AplProcessoNotificacao;
import br.ifes.leds.sincap.web.model.UsuarioSessao;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Locale;

import static br.ifes.leds.sincap.controleInterno.cln.cdp.Sexo.MASCULINO;
import static br.ifes.leds.sincap.web.controller.ContextUrls.APP_NOTIFICACAO_OBITO;
import static br.ifes.leds.sincap.web.controller.ContextUrls.EDITAR;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ObitoControllerTest extends AbstractionTest {

    private MockMvc mockMvc;
    private MockHttpSession session;

    @Autowired
    private AplProcessoNotificacao aplProcessoNotificacao;
    @Autowired
    private AplCadastroInterno aplCadastroInterno;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setUp() {
        Mockito.reset(aplProcessoNotificacao, aplCadastroInterno);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        session = new MockHttpSession();
        session.setAttribute("user", UsuarioSessao.builder()
                .idUsuario(1L)
                .cpfUsuario("111.111.111-11")
                .idHospital(1L)
                .nome("Notificador 1")
                .build());
    }

    @Test
    @SneakyThrows
    public void novaNotificacao() {
        mockMvc.perform(get("/obito/adicionar")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("listaEstadoItem"))
                .andExpect(model().attributeExists("listaSetor"))
                .andExpect(model().attributeExists("listaCausaNaoDoacao"))
                .andExpect(model().attribute("sucessoObito", true))
                .andExpect(view().name("form-notificacao-obito"));

        verify(aplCadastroInterno, times(1)).obterSetorPorHospital(1L);
        verify(aplCadastroInterno, times(1)).obterCausaNaoDoacaoContraIndMedica();
        verifyNoMoreInteractions(aplCadastroInterno);
        verifyZeroInteractions(aplProcessoNotificacao);
    }

    @Test
    @SneakyThrows
    public void salvarNotificacaoSucessoAptoDoacao() {
        mockMvc.perform(post("/obito/salvar")
                .session(session)
                .accept(MediaType.APPLICATION_FORM_URLENCODED)
                .param("obito.paciente.nome", "hjrasncdnbnsdgç")
                .param("obito.paciente.dataNascimento", "27/09/1991")
                .param("obito.paciente.dataInternacao", "27/01/2013")
                .param("obito.paciente.documentoSocial", "543534242")
                .param("obito.paciente.telefone.numero", "(27) 99991-4100")
                .param("obito.paciente.sexo", "MASCULINO")
                .param("obito.paciente.numeroSUS", "657634234")
                .param("obito.paciente.profissao", "Eletricista")
                .param("obito.paciente.nacionalidade", "Brasileira")
                .param("obito.paciente.numeroProntuario", "90875299")
                .param("obito.paciente.nomeMae", "Mãe de Fulano")
                .param("obito.paciente.endereco.estado", "8")
                .param("obito.paciente.endereco.cidade", "2052")
                .param("obito.paciente.endereco.bairro", "3251")
                .param("obito.paciente.endereco.cep", "29182-527")
                .param("obito.paciente.endereco.logradouro", "")
                .param("obito.dataObito", "27/09/2014 22:30")
                .param("obito.setor", "1")
                .param("obito.primeiraCausaMortis", "Primeira Causa")
                .param("obito.segundaCausaMortis", "Segunda Causa")
                .param("obito.terceiraCausaMortis", "")
                .param("obito.quartaCausaMortis", "")
                .param("obito.aptoDoacao", "true")
                .param("causaNaoDoacao", ""))
                .andExpect(redirectedUrl("/index?sucessoObito=true"));

        ArgumentCaptor<ProcessoNotificacaoDTO> argumentCaptor = ArgumentCaptor.forClass(ProcessoNotificacaoDTO.class);

        verify(aplProcessoNotificacao, times(1)).salvarNovaNotificacao(argumentCaptor.capture(), isA(Long.class));
        verifyNoMoreInteractions(aplProcessoNotificacao);
        verifyZeroInteractions(aplCadastroInterno);

        ProcessoNotificacaoDTO notificacaoDTO = argumentCaptor.getValue();

        assertTrue(notificacaoDTO.getObito().getPaciente().getNome().equals("hjrasncdnbnsdgç"));
        assertNotNull(notificacaoDTO.getObito().getPaciente().getDataInternacao());
        assertNotNull(notificacaoDTO.getObito().getPaciente().getDataNascimento());
        assertNotNull(notificacaoDTO.getObito().getDataObito());
    }

    @Test
    @SneakyThrows
    public void salvarNotificacaoSucessoInaptoDoacao() {
        mockMvc.perform(post("/obito/salvar")
                .session(session)
                .accept(MediaType.APPLICATION_FORM_URLENCODED)
                .param("obito.paciente.nome", "hjrasncdnbnsdgç")
                .param("obito.paciente.dataNascimento", "27/09/1991")
                .param("obito.paciente.dataInternacao", "27/01/2013")
                .param("obito.paciente.documentoSocial", "543534242")
                .param("obito.paciente.telefone.numero", "(27) 99991-4100")
                .param("obito.paciente.sexo", "MASCULINO")
                .param("obito.paciente.numeroSUS", "657634234")
                .param("obito.paciente.profissao", "Eletricista")
                .param("obito.paciente.nacionalidade", "Brasileira")
                .param("obito.paciente.numeroProntuario", "90875299")
                .param("obito.paciente.nomeMae", "Mãe de Fulano")
                .param("obito.paciente.endereco.estado", "8")
                .param("obito.paciente.endereco.cidade", "2052")
                .param("obito.paciente.endereco.bairro", "3251")
                .param("obito.paciente.endereco.cep", "29182-527")
                .param("obito.paciente.endereco.logradouro", "")
                .param("obito.dataObito", "27/09/2014 22:30")
                .param("obito.setor", "1")
                .param("obito.primeiraCausaMortis", "Primeira Causa")
                .param("obito.segundaCausaMortis", "Segunda Causa")
                .param("obito.terceiraCausaMortis", "")
                .param("obito.quartaCausaMortis", "")
                .param("obito.aptoDoacao", "false")
                .param("causaNaoDoacao", "1"))
                .andExpect(redirectedUrl("/index?sucessoObito=true"));

        ArgumentCaptor<ProcessoNotificacaoDTO> argumentCaptor = ArgumentCaptor.forClass(ProcessoNotificacaoDTO.class);

        verify(aplProcessoNotificacao, times(1)).salvarNovaNotificacao(argumentCaptor.capture(), isA(Long.class));
        verifyNoMoreInteractions(aplProcessoNotificacao);
        verifyZeroInteractions(aplCadastroInterno);

        ProcessoNotificacaoDTO notificacaoDTO = argumentCaptor.getValue();

        assertTrue(notificacaoDTO.getObito().getPaciente().getNome().equals("hjrasncdnbnsdgç"));
        assertNotNull(notificacaoDTO.getObito().getPaciente().getDataInternacao());
        assertNotNull(notificacaoDTO.getObito().getPaciente().getDataNascimento());
        assertNotNull(notificacaoDTO.getObito().getDataObito());
        assertTrue(1L == notificacaoDTO.getCausaNaoDoacao());
    }

    @Test
    @SneakyThrows
    public void salvarNotificacaoConstraintViolation() {
        mockMvc.perform(post("/obito/salvar")
                .session(session)
                .accept(MediaType.APPLICATION_FORM_URLENCODED)
                .param("obito.paciente.nome", "hjrasncdnbnsdgç")
//                Data de nascimento mais recente do que internação
                .param("obito.paciente.dataNascimento", "27/09/1991")
                .param("obito.paciente.dataInternacao", "27/01/2013")
                .param("obito.paciente.documentoSocial", "543534242")
                .param("obito.paciente.telefone.numero", "(27) 99991-4100")
//                Sexo null
                .param("obito.paciente.sexo", "")
//                Numero SUS null
                .param("obito.paciente.numeroSUS", "")
                .param("obito.paciente.profissao", "Eletricista")
                .param("obito.paciente.nacionalidade", "Brasileira")
                .param("obito.paciente.numeroProntuario", "90875299")
                .param("obito.paciente.nomeMae", "Mãe de Fulano")
                .param("obito.paciente.endereco.estado", "8")
                .param("obito.paciente.endereco.cidade", "2052")
                .param("obito.paciente.endereco.bairro", "3251")
                .param("obito.paciente.endereco.cep", "29182-527")
                .param("obito.paciente.endereco.logradouro", "")
                .param("obito.dataObito", "27/09/2014 22:30")
                .param("obito.setor", "1")
                .param("obito.primeiraCausaMortis", "Primeira Causa")
                .param("obito.segundaCausaMortis", "Segunda Causa")
                .param("obito.terceiraCausaMortis", "")
                .param("obito.quartaCausaMortis", "")
                .param("obito.aptoDoacao", "false")
                .param("causaNaoDoacao", "1"))

                .andExpect(view().name("form-notificacao-obito"))
                .andExpect(model().attributeExists("listaSetor"))
                .andExpect(model().attributeExists("listaCausaNaoDoacao"))
                .andExpect(model().attributeExists("listaEstadoItem"))
                .andExpect(model().attributeExists("listaCidadeItem"))
                .andExpect(model().attributeExists("listaBairroItem"))
                .andExpect(model().attributeExists("fieldErrors"))
                .andExpect(model().attribute("erro", is(true)))
                .andExpect(model().attribute("processo", Matchers.isA(ProcessoNotificacaoDTO.class)));
    }

    @Test
    @SneakyThrows
    public void editarNotificacao() {
        ProcessoNotificacaoDTO processoNotificacaoDTO = criarProcesso();
        when(aplProcessoNotificacao.obter(isA(Long.class))).thenReturn(processoNotificacaoDTO);

        mockMvc.perform(get(APP_NOTIFICACAO_OBITO + EDITAR + "/22")
                .session(session))

                .andExpect(view().name("form-notificacao-obito"))
                .andExpect(model().attributeExists("listaSetor"))
                .andExpect(model().attributeExists("listaCausaNaoDoacao"))
                .andExpect(model().attributeExists("listaEstadoItem"))
                .andExpect(model().attributeExists("listaCidadeItem"))
                .andExpect(model().attributeExists("listaBairroItem"))
                .andExpect(model().attribute("processo", sameInstance(processoNotificacaoDTO)));

        verify(aplProcessoNotificacao, times(1)).obter(isA(Long.class));
    }

    private static ProcessoNotificacaoDTO criarProcesso() {
        return ProcessoNotificacaoDTO.builder()
                .obito(ObitoDTO.builder()
                        .paciente(PacienteDTO.builder()
                                .endereco(EnderecoDTO.builder()
                                        .estado(8L)
                                        .cidade(2052L)
                                        .bairro(3251L)
                                        .logradouro("Rua Tal")
                                        .cep("29182-527")
                                        .numero("5324")
                                        .build())
                                .nome("Fulano de Tal")
                                .sexo(MASCULINO)
                                .dataNascimento(new DateTime(1991, 9, 27, 0, 0).toCalendar(Locale.getDefault()))
                                .dataInternacao(new DateTime(2014, 1, 10, 0, 0).toCalendar(Locale.getDefault()))
                                .documentoSocial("15663477")
                                .build())
                        .setor(1L)
                        .build())
                .build();
    }
}