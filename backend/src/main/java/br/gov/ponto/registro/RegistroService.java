package br.gov.ponto.registro;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.cadastro.GeofenceLocalService;
import br.gov.ponto.cadastro.RegrasPontoService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Geofence;
import br.gov.ponto.cadastro.domain.RegrasEfetivas;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.api.BatidaResponse;
import br.gov.ponto.registro.api.ComprovanteResponse;
import br.gov.ponto.registro.api.RegistrarPontoRequest;
import br.gov.ponto.registro.domain.CadeiaHash;
import br.gov.ponto.registro.domain.DeducaoBatida;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.rep.HashMarcacaoRep;
import br.gov.ponto.relatorios.rep.MontadorAfd;
import br.gov.ponto.registro.domain.TipoMarcacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RegistroService {

    private final RegistroPontoRepository registroRepository;
    private final VinculoRepository vinculoRepository;
    private final RegrasPontoService regrasPontoService;
    private final GeofenceLocalService geofenceLocalService;
    private final NsrGenerator nsrGenerator;
    private final AuditoriaService auditoriaService;
    private final br.gov.ponto.cadastro.ServidorRepository servidorRepository;

    public RegistroService(RegistroPontoRepository registroRepository,
                           VinculoRepository vinculoRepository,
                           RegrasPontoService regrasPontoService,
                           GeofenceLocalService geofenceLocalService,
                           NsrGenerator nsrGenerator,
                           AuditoriaService auditoriaService,
                           br.gov.ponto.cadastro.ServidorRepository servidorRepository) {
        this.registroRepository = registroRepository;
        this.vinculoRepository = vinculoRepository;
        this.regrasPontoService = regrasPontoService;
        this.geofenceLocalService = geofenceLocalService;
        this.nsrGenerator = nsrGenerator;
        this.auditoriaService = auditoriaService;
        this.servidorRepository = servidorRepository;
    }

    /** Registro com tipo informado pelo cliente (web/totem/sync offline legado). */
    @Transactional
    public ComprovanteResponse registrar(RegistrarPontoRequest request) {
        UUID tenantId = TenantContext.requireCurrent();

        var existente = registroRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
        if (existente.isPresent()) {
            return ComprovanteResponse.from(existente.get());
        }
        exigirVinculo(request.vinculoId(), tenantId);

        RegistroPonto registro = salvar(tenantId, request.vinculoId(), request.tipo(), request.origem(),
                request.dataHoraDispositivo(), request.latitude(), request.longitude(),
                request.offline(), request.idempotencyKey(), false, "");
        return ComprovanteResponse.from(registro);
    }

    /**
     * "Botao unico": o servidor deduz o tipo da batida (entrada/intervalo/saida) pela
     * sequencia do dia + as regras efetivas do vinculo (jornada do orgao). Tambem
     * sinaliza a batida como "fora da cerca" quando o orgao tem geofence e a
     * localizacao esta fora do raio. Mantem NSR + idempotencia + auditoria.
     */
    @Transactional
    public BatidaResponse bater(BaterPontoRequest request) {
        UUID tenantId = TenantContext.requireCurrent();

        var existente = registroRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
        if (existente.isPresent()) {
            return BatidaResponse.from(existente.get());
        }
        exigirVinculo(request.vinculoId(), tenantId);

        LocalDate hoje = LocalDate.now(TempoMunicipal.ZONE);
        Instant[] dia = TempoMunicipal.intervaloDoDia(hoje);
        int batidasHoje = registroRepository
                .findByVinculoIdAndTenantIdAndDataHoraServidorBetweenOrderByDataHoraServidor(
                        request.vinculoId(), tenantId, dia[0], dia[1])
                .size();

        RegrasEfetivas regras = regrasPontoService.efetivasParaVinculo(request.vinculoId());
        TipoMarcacao tipo = DeducaoBatida.proximoTipo(batidasHoje, regras.comIntervalo());
        // Geofence é apenas verificação para o administrador: nunca bloqueia nem alerta o servidor.
        // A localização e o indicador "fora da área" ficam no registro para o admin conferir.
        boolean foraDaCerca = foraDaArea(regras, request.vinculoId(), request.latitude(), request.longitude());

        RegistroPonto registro = salvar(tenantId, request.vinculoId(), tipo, request.origem(),
                request.dataHoraDispositivo(), request.latitude(), request.longitude(),
                request.offline(), request.idempotencyKey(), foraDaCerca,
                " (deduzido)" + (foraDaCerca ? " fora-da-cerca" : ""));
        return BatidaResponse.from(registro);
    }

    /**
     * Cria uma marcação de correção ("esqueci de bater" aprovado ou correção do RH).
     * Registros são imutáveis: a correção é uma NOVA batida encadeada na cadeia de hash,
     * com {@code dataHoraServidor} no momento pretendido (a apuração ordena por ele) e
     * origem AJUSTE para rastreabilidade. Nunca edita um registro existente.
     */
    @Transactional
    public RegistroPonto registrarCorrecao(UUID vinculoId, Instant dataHoraServidor,
                                           TipoMarcacao tipo, String motivo) {
        UUID tenantId = TenantContext.requireCurrent();
        exigirVinculo(vinculoId, tenantId);

        long nsr = nsrGenerator.next(tenantId);
        String idempotencyKey = "correcao-" + UUID.randomUUID();
        String hashAnterior = registroRepository.findTopByTenantIdOrderByNsrDesc(tenantId)
                .map(RegistroPonto::getHash).orElse("");
        String hash = CadeiaHash.calcular(hashAnterior, tenantId, vinculoId, nsr, tipo,
                dataHoraServidor, idempotencyKey);
        RegistroPonto registro = new RegistroPonto(tenantId, vinculoId, nsr, tipo, OrigemRegistro.AJUSTE,
                dataHoraServidor, dataHoraServidor, null, null, false, idempotencyKey, false);
        registro.definirCadeia(hashAnterior, hash);
        // Hash oficial do REP-P (art. 79, VIII): o MESMO valor vai para o comprovante do
        // trabalhador e para o campo 8 do registro tipo "7" do AFD. Calculado agora e nunca
        // recalculado — se comprovante e AFD divergissem, a fiscalizacao teria razao em recusar.
        registro.definirHashRep(hashRepDe(tenantId, vinculoId, registro));
        registro = registroRepository.save(registro);
        auditoriaService.registrar("CORRECAO_MARCACAO", "registro", registro.getId().toString(),
                "NSR " + nsr + " " + tipo + " em " + dataHoraServidor
                        + (motivo != null ? " (correcao: " + motivo + ")" : " (correcao)"));
        return registro;
    }

    @Transactional(readOnly = true)
    public List<ComprovanteResponse> listarPorVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return registroRepository.findByVinculoIdAndTenantIdOrderByNsr(vinculoId, tenantId).stream()
                .map(ComprovanteResponse::from).toList();
    }

    /**
     * Localização fora da área do órgão: só marca "fora" quando há ao menos uma área de referência
     * (a cerca primária do órgão e/ou os locais volantes — multi-geofence) e a localização está
     * fora de TODAS. Sem localização, sem área, ou em teletrabalho → nunca fora. Apenas verificação
     * do administrador: nunca bloqueia nem alerta o servidor.
     */
    private boolean foraDaArea(RegrasEfetivas regras, UUID vinculoId,
                               BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null || regras.teletrabalho()) {
            return false;
        }
        List<Geofence> areas = new ArrayList<>();
        regras.geofence().ifPresent(areas::add); // cerca primária do órgão
        areas.addAll(geofenceLocalService.areasDoVinculo(vinculoId)); // locais volantes adicionais
        if (areas.isEmpty()) {
            return false;
        }
        double lat = latitude.doubleValue();
        double lng = longitude.doubleValue();
        return areas.stream().noneMatch(a -> a.contem(lat, lng));
    }

    private RegistroPonto salvar(UUID tenantId, UUID vinculoId, TipoMarcacao tipo, OrigemRegistro origem,
                                 Instant dataHoraDispositivo, BigDecimal latitude, BigDecimal longitude,
                                 boolean offline, String idempotencyKey, boolean foraDaCerca, String auditSuffix) {
        long nsr = nsrGenerator.next(tenantId);
        Instant agora = Instant.now();
        // Cadeia de integridade: encadeia o hash do último registro do tenant.
        String hashAnterior = registroRepository.findTopByTenantIdOrderByNsrDesc(tenantId)
                .map(RegistroPonto::getHash).orElse("");
        String hash = CadeiaHash.calcular(hashAnterior, tenantId, vinculoId, nsr, tipo, agora, idempotencyKey);
        RegistroPonto registro = new RegistroPonto(
                tenantId, vinculoId, nsr, tipo, origem,
                agora, dataHoraDispositivo, latitude, longitude, offline, idempotencyKey, foraDaCerca);
        registro.definirCadeia(hashAnterior, hash);
        // Hash oficial do REP-P (art. 79, VIII): o MESMO valor vai para o comprovante do
        // trabalhador e para o campo 8 do registro tipo "7" do AFD. Calculado agora e nunca
        // recalculado — se comprovante e AFD divergissem, a fiscalizacao teria razao em recusar.
        registro.definirHashRep(hashRepDe(tenantId, vinculoId, registro));
        registro = registroRepository.save(registro);
        auditoriaService.registrar("REGISTRO_PONTO", "registro", registro.getId().toString(),
                "NSR " + registro.getNsr() + " " + registro.getTipo() + auditSuffix);
        return registro;
    }

    /** Calcula o hash oficial da marcacao, encadeando com o hash da marcacao anterior do ente. */
    private String hashRepDe(UUID tenantId, UUID vinculoId, RegistroPonto registro) {
        String cpf = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .flatMap(v -> servidorRepository.findByIdAndTenantId(v.getServidorId(), tenantId))
                .map(br.gov.ponto.cadastro.domain.Servidor::getCpf)
                .orElse("");
        String hashRepAnterior = registroRepository.findTopByTenantIdOrderByNsrDesc(tenantId)
                .map(RegistroPonto::getHashRep).orElse("");
        return HashMarcacaoRep.calcular(registro.getNsr(), registro.instanteDaMarcacao(), cpf,
                registro.instanteDaGravacao(), coletorDe(registro.getOrigem()), registro.isOffline(),
                hashRepAnterior == null ? "" : hashRepAnterior);
    }

    /** Identificador do coletor da marcacao (campo 6 do registro tipo "7"). */
    private static MontadorAfd.Coletor coletorDe(OrigemRegistro origem) {
        return switch (origem) {
            case MOBILE -> MontadorAfd.Coletor.APLICATIVO_MOBILE;
            case WEB -> MontadorAfd.Coletor.BROWSER;
            case TOTEM -> MontadorAfd.Coletor.DISPOSITIVO_ELETRONICO;
            case AJUSTE -> MontadorAfd.Coletor.OUTRO;
        };
    }

    private void exigirVinculo(UUID vinculoId, UUID tenantId) {
        if (!vinculoRepository.existsByIdAndTenantId(vinculoId, tenantId)) {
            throw new IllegalArgumentException("Vinculo inexistente no ente");
        }
    }
}
