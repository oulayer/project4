# Grand Line - Frutas e Haki (Forge 1.20.1)

## Compilar
1. Instale o JDK 17.
2. Copie `gradlew`, `gradlew.bat` e a pasta `gradle/` do MDK oficial do Forge 1.20.1 (47.2.x) para esta pasta
   (ou rode `gradle wrapper` se tiver o Gradle instalado).
3. `./gradlew build`  ->  jar em `build/libs/`. Para testar: `./gradlew runClient`.

## Controles (alteráveis em Opções > Controles > Grand Line)
| Tecla | Ação |
|---|---|
| Z / X / C / R | 4 habilidades da fruta |
| G | Haki do Armamento |
| M | Endurecimento (precisa do Armamento ligado) |
| V | Haki da Observação |
| B | Haki do Rei |
| N | Corpo de Logia (Chamas e Gelo) |
| H | Soru (dash) |
| Y | Geppo (até 3 pulos no ar) |
| K | Rankyaku (lâmina de ar) |

Combate corpo a corpo: acerte golpes seguidos com o botão esquerdo para fazer combo de 4 golpes (jab, direto, gancho e impacto final).

## Comandos (op)
/grandline set <rubber|flame|ice|quake>   /grandline reset

## Compilar sem instalar nada (GitHub)
1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta (incluindo `.github`).
2. Abra a aba **Actions**, aguarde o build "Build mod" terminar (uns 5 min).
3. Entre na execução e baixe o artefato **grandline-jar**: dentro está o .jar do mod.

## v1.2 - Estilo da Mera Mera
Com a **Fruta de Chamas** e a **mão vazia**, o clique esquerdo vira um combo de fogo de 5 golpes
(Brasa, Cruzado de Fogo, Roda de Chamas, Gancho Flamejante e Hiken Enjo). Olhando para baixo no ar,
o ataque vira um Mergulho de Fogo. Para minerar blocos, o clique funciona normalmente.
O Hiken (Z) agora é uma entidade com modelo 3D próprio. O corpo de fogo (Logia, tecla N) e o braço de fogo
aparecem no modelo do jogador, para você e para quem está perto.
