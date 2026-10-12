# saftao-java — desenho da API (rascunho para aprovação)

> **Estado: proposta.** Nada disto está implementado, e os exemplos **não foram compilados**. Servem para avaliar o desenho do ponto de vista de quem usa a biblioteca. Só depois de aprovado se implementa e se actualiza `saftao-spec/spec/05-api.md`.

## Decisões tomadas (2026-10-12)

- **Fase 2 só facturação.** A contabilidade (GL) é importante e vem logo a seguir (fase 4, prioritária).
- **Nomes em inglês**, como o XSD e a API da AGT, incluindo os `enum`.
- **Mensagens em `pt_AO` (por defeito) e `en`.** Os códigos não mudam com a língua.
- **Só `BigDecimal`** para valores, sem `String` nos construtores, por rigor.
- **Pacotes `ao.bitubi.saftao`** (o `solutions.bitubi` só existia por causa do nome já ocupado no GitHub). O `groupId` Maven passa a `ao.bitubi`; publicar no Maven Central exige provar a posse do domínio `bitubi.ao`.
- **Fase 1 implementada** (esta branch): pacotes, relatório com gravidade, cobertura, código, caminho, mensagens, entradas `Path`/`File`/`InputStream`/`byte[]`. Ficam para depois, porque ainda não têm comportamento: o modo `STRICT` (fase 3) e o aviso de codificação `ENC-001`.
- Desvios face ao texto abaixo: são **53** regras na especificação (não 52); os problemas vêm por ordem de linha (os sem linha no fim); não há ainda classe `Validator` (só `SaftAo.validate`), entra com o modo estrito; `getCode()` dos erros de esquema é a família do erro (`XSD:cvc-pattern-valid`, `XSD:cvc-identity-constraint`), igual em todos os JDK.

## Decisões a que isto responde

1. **Âmbito:** a biblioteca **gera e valida** o SAF-T (AO), tudo o que um ERP precisa. A assinatura fica, marcada experimental.
2. **O que "válido" significa:** o relatório passa a dizer **o que foi verificado**. Deixa de existir um `isValid()` que engana.
3. **Gravidade:** cada problema é `ERROR` ou `WARNING`. O modo estrito acrescenta avisos baseados nas observações de terceiros.

## Princípios de DX

- **Impossível produzir um ficheiro com a estrutura errada.** A ordem das secções do XSD é imposta pela API, não por documentação.
- **Os totais de cabeçalho calculam-se sozinhos** (`NumberOfEntries`, `TotalDebit`, `TotalCredit`, totais por tipo). Quem escreve só fornece documentos.
- **Memória constante.** Um ERP tem milhões de linhas, por isso a escrita faz streaming. O modo em memória é uma conveniência para ficheiros pequenos e testes.
- **Não esconde regras fiscais.** A biblioteca não arredonda nem calcula impostos às escondidas. Quem emite fornece os valores. O validador diz se não batem. Ajudas de cálculo, se vierem, são opcionais e separadas.
- **Erros que se podem acção.** Cada problema traz código estável, caminho do elemento, linha e coluna, e a regra da especificação.
- **Java 8:** sem `record`, `var` nem `List.of`. Classes imutáveis, construtores fluentes, `BigDecimal` para dinheiro, `enum` para códigos fechados, `java.time` para datas.
- **Excepções:** só para erro de uso (argumento impossível, API chamada fora de ordem), sempre não verificadas. Problemas **do ficheiro** vão para o relatório, nunca para excepções.

## 1. Validar (o caso mais simples)

```java
ValidationReport report = SaftAo.validate(Paths.get("saft-2026-09.xml"));

if (report.hasErrors()) {
    for (ValidationIssue e : report.getErrors()) {
        System.out.println(e);   // R-009 SalesInvoices/NumberOfEntries (linha 14): esperado 120, encontrado 119
    }
}
System.out.println(report.getCoverage()); // Esquema ✔ · Regras 3 de 53 · Assinatura ✘ (não pedida)
```

O que muda face ao 0.1:

| Hoje | Proposto |
|---|---|
| `isValid()` | **removido**. `hasErrors()` diz só se há erros nas verificações feitas. |
| sem cobertura | `getCoverage()` diz o que correu e o que não está implementado. |
| um nível de problema | `ERROR` e `WARNING`. |
| `ValidationIssue.Level` (SCHEMA / BUSINESS_RULE) | `source`: `SCHEMA`, `RULE`, `OBSERVATION`. A gravidade é à parte. |

```java
public final class ValidationReport {
    boolean hasErrors();
    List<ValidationIssue> getErrors();
    List<ValidationIssue> getWarnings();
    List<ValidationIssue> getIssues();           // todos, por ordem no ficheiro
    Coverage getCoverage();
}

public final class Coverage {
    boolean isSchemaChecked();
    Set<String> getRulesChecked();               // ex.: R-009, R-013, R-050
    Set<String> getRulesNotImplemented();        // as outras 50, para quem quer saber
    boolean isComplete();                        // esquema + todas as regras da especificação
}

public final class ValidationIssue {
    Severity getSeverity();                      // ERROR | WARNING
    Source getSource();                          // SCHEMA | RULE | OBSERVATION
    String getCode();                            // "R-009", "O-004", "XSD-InvoiceNoConstraint"
    String getMessage();                         // português; mensagem original do motor em getDetail()
    String getPath();                            // "SourceDocuments/SalesInvoices/Invoice[3]/Line[2]"
    int getLine(); int getColumn();              // -1 se desconhecido
}
```

`Coverage.isComplete()` é a resposta honesta à pergunta "isto está mesmo válido?". Enquanto só há 3 regras de 52, `isComplete()` é `false` e o `toString()` do relatório diz-o (`SEM ERROS, MAS COBERTURA PARCIAL: 3 de 53 regras`).

### Modo estrito

```java
ValidationReport r = SaftAo.validator()
        .mode(Mode.STRICT)          // acrescenta avisos O-xxx (observações de terceiros)
        .validate(input);
```

- `STANDARD` (por defeito): esquema e regras normativas da especificação. Só `ERROR`.
- `STRICT`: soma os avisos das observações (cálculo de imposto, texto de isenção, descontos, etc.). São **avisos**, nunca erros, porque não estão confirmados pela AGT. Cada um traz o código `O-xxx` para quem quiser ver a origem.

Entradas aceites: `Path`, `File`, `InputStream`, `byte[]`. Uma codificação diferente de UTF-8 é um `WARNING` explícito (`ENC-001`), não um nome corrompido.

## 2. Gerar (streaming)

```java
Header header = Header.builder()
        .auditFileVersion("1.01_01")
        .company(Company.builder().nif("5000000123").name("Exemplo, Lda").build())
        .fiscalYear(2026)
        .period(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        .currency("AOA")
        .fileType(FileType.FACTURACAO)
        .software(Software.of("Meu ERP", "1.0.1", "C_134"))
        .build();

try (SaftWriter w = SaftAo.writer(Files.newOutputStream(out), header)) {

    w.masterFiles()                      // a ordem do XSD é imposta: só se avança por esta cadeia
        .customers(customerStream)       // Iterable<Customer>: lê da base de dados à medida
        .products(productStream)
        .taxTable(taxTable)
        .done();

    w.salesInvoices()
        .invoices(invoiceIterable)       // Iterable<SalesInvoice>
        .done();
}                                        // close(): escreve os totais e fecha o XML

// opcional, mas recomendado: confirmar o que foi escrito
ValidationReport report = SaftAo.validate(out);
```

- Chamar `salesInvoices()` antes de `masterFiles().done()` lança `IllegalStateException` com mensagem a dizer a ordem esperada.
- `close()` calcula e escreve `NumberOfEntries`, `TotalDebit` e `TotalCredit`. Como o XML tem o cabeçalho **antes** dos documentos, a implementação escreve os documentos num ficheiro temporário ou faz duas passagens; decisão técnica a tomar na implementação.
- Para testes e ficheiros pequenos: `SaftAo.build(header).…toBytes()` em memória.

### Os documentos

```java
SalesInvoice inv = SalesInvoice.builder()
        .number("FT A/1")                          // validado: padrão "tipo série/número"
        .type(InvoiceType.FT)
        .date(LocalDate.of(2026, 9, 3))
        .systemEntry(LocalDateTime.of(2026, 9, 3, 10, 15, 30))
        .customerId("C001")
        .addLine(Line.builder()
                .product("P001", "Produto 1")
                .quantity("2").unitPrice("250.00")
                .credit("500.00")
                .tax(Tax.iva(TaxCode.NOR, "14").amount("70.00"))
                .build())
        .totals(Totals.builder().taxPayable("70.00").netTotal("500.00").grossTotal("570.00").build())
        .build();
```

- Valores monetários são sempre `BigDecimal`, nunca `double` nem `String`.
- `build()` falha **cedo** com `IllegalArgumentException` para o que é impossível (data nula, NIF vazio, número sem o padrão). As regras fiscais (somas, NC acima da origem) **não** falham aqui: ficam para o validador.
- Os nomes seguem os do XSD em inglês (`customerId`, `unitPrice`), para quem tem de consultar a especificação.
- **Atenção:** os exemplos abaixo ainda mostram valores como `String` (`"250.00"`) e pacotes `solutions.bitubi`. Pela decisão acima passam a `new BigDecimal("250.00")` e `ao.bitubi`.

### Ajuda de cálculo (opcional, fase posterior)

```java
Totals t = Totals.of(inv.getLines());   // soma o que foi dado; NÃO calcula impostos nem arredonda
```

Calcular imposto a partir de preço e taxa depende de regras de arredondamento que ainda não temos confirmadas. Não entra na v1 sem fonte oficial (a documentação da API da AGT diz que o imposto da linha se arredonda **por excesso** ao cêntimo; fica registado para esta ajuda).

## 3. Assinar (experimental)

```java
@Experimental   // pode mudar: ver spec/04-signature.md
InvoiceSigner signer = InvoiceSigner.withKey(PemKeys.privateKey(pem), /*keyVersion*/ 1);

SalesInvoice signed = signer.sign(inv, previous);  // devolve nova cópia com Hash e HashControl
```

- A cadeia é por série e tipo; `previous` é o documento anterior da mesma série (ou `null` na primeira).
- Não guarda estado nem chaves: recebe-as.
- Marcada experimental porque a fonte é uma proposta de 2018 e o regime de facturação electrónica usa outro mecanismo.

## 4. Linha de comandos

```bash
java -jar saftao-cli.jar validate saft.xml            # texto legível, código de saída 1 se houver erros
java -jar saftao-cli.jar validate --strict --json saft.xml
```

Módulo separado (`saftao-cli`), para a biblioteca principal continuar sem dependências.

## 5. Estrutura de pacotes

```
ao.bitubi.saftao            SaftAo (fachada), enums públicos
ao.bitubi.saftao.model      Header, Customer, Product, SalesInvoice, Line, Tax, Totals, ...
ao.bitubi.saftao.write      SaftWriter e as etapas (MasterFilesStage, ...)
ao.bitubi.saftao.validate   Validator, ValidationReport, Coverage, ValidationIssue
ao.bitubi.saftao.sign       InvoiceSigner, PemKeys   (@Experimental)
```

Os pacotes de implementação (`rules`, `internal`) não fazem parte da API pública.

## 6. Fases de implementação sugeridas

1. **Relatório novo** (gravidade, cobertura, código, caminho). Pequena alteração e desbloqueia o resto.
2. **Escrita da parte de facturação:** cabeçalho, clientes, produtos, tabela de impostos, facturas de venda. Validada contra o XSD e contra os casos de conformidade.
3. **Resto das regras** (50 em falta) e modo estrito.
4. **Restantes secções:** contabilidade (GL), documentos de transporte, de conferência, pagamentos, compras.
5. **Assinatura** na nova forma, depois de confirmado o regime.
6. **CLI** e publicação.

## Perguntas em aberto

- Nenhuma de âmbito. Dúvidas técnicas ficam para a implementação da fase 2 (por exemplo, ficheiro temporário ou duas passagens para escrever os totais do cabeçalho).
