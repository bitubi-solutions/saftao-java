# saftao-java

Biblioteca Java para validar e assinar o ficheiro SAF-T (AO), por Bitubi Solutions.

Implementa a especificação de [`saftao-spec`](https://github.com/bitubi-solutions/saftao-spec) e passa os seus testes de conformidade.

> **Aviso.** Este projecto **não é um programa de facturação certificado** e **não foi avaliado, aprovado nem endossado pela AGT** (Administração Geral Tributária). Quem o integrar num software que emite documentos fiscais é responsável por cumprir os requisitos legais aplicáveis, incluindo a certificação do software e o registo como produtor. A assinatura incluída serve para teste e interoperabilidade. Fornecido "tal como está", sem garantias (ver licença).

- **Java 8 ou superior.** Compilada com `--release 8` e testada num JDK 8 real e no JDK 25.
- **Sem dependências** em tempo de execução: usa só o JDK (`javax.xml.validation`, StAX, `java.security`).
- **Segura por omissão:** rejeita `DOCTYPE`, não resolve entidades externas e não acede à rede.

## Estado: v0.1 (mínima)

| Nível | O que faz | Estado |
|---|---|---|
| 1. Esquema | Valida contra o XSD SAF-T (AO) 1.01_01, incluindo unicidade (`xs:unique`) e referências (`xs:keyref`) | ✅ |
| 2. Regras de negócio | Regras de `spec/03-business-rules.md` | 🟡 3 de 52 (R-009, R-013, R-050) |
| 3. Assinatura | Constrói a mensagem a assinar, assina e verifica o `Hash` (RSA, SHA-1, PKCS#1 v1.5) | ✅ |
| Modelo / geração de ficheiros | Classes para construir um SAF-T | ❌ ainda não |

Os parâmetros de assinatura vêm de uma **proposta oficial de 2018**; ver as ressalvas em
[`spec/04-signature.md`](https://github.com/bitubi-solutions/saftao-spec/blob/main/spec/04-signature.md)
antes de usar em produção.

## Uso

Validar um ficheiro:

```java
ValidationReport report = SaftAo.validate(Paths.get("saft.xml"));
if (!report.isValid()) {
    for (ValidationIssue issue : report.getIssues()) {
        System.out.println(issue);   // por exemplo: BUSINESS_RULE[R-009] (linha 63): NumberOfEntries (3) ...
    }
}
```

Assinar um documento (campo `Hash`):

```java
PrivateKey key = PemKeys.readPrivateKey(pem);              // aceita "RSA PRIVATE KEY" (PKCS#1) e "PRIVATE KEY" (PKCS#8)
String total = SigningMessage.formatGrossTotal(new BigDecimal("1200"));      // "1200.00"
String message = SigningMessage.build("2018-05-18", "2018-05-18T11:22:19", "FAC 001/18", total, previousHash);
String hash = DocumentSigner.sign(key, message);          // base64, 172 caracteres com chave de 1024 bits
```

`previousHash` é o `Hash` do documento anterior da mesma série, ou vazio no primeiro.

## Compilar e testar

Os testes de conformidade vêm do repo `saftao-spec`, que deve estar ao lado deste:

```
bitubi/
├── saftao-spec/
└── saftao-java/
```

```bash
mvn test                                  # usa ../saftao-spec
mvn test -Dsaftao.spec.dir=/outro/caminho
```

## Segurança

SHA-1 e RSA de 1024 bits são fracos hoje. Esta biblioteca usa-os **apenas** no campo `Hash`, porque é o que a AGT define. Não os reutilize noutro contexto.

## Licença

Apache-2.0. Ver [LICENSE](LICENSE) e [NOTICE](NOTICE) (o XSD embebido é de terceiros).
