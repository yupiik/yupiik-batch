package io.yupiik.batch.runtime.documentation;

import io.yupiik.batch.runtime.component.mapping.Mapping;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public record MapperDocGenerator(Class<?> mapper, String tablePrefix) implements Supplier<String> {
    @Override
    public String get() {
        final var conf = mapper.getAnnotation(Mapping.class);
        // todo: hierarchy(mapper) instead of just mapper to have inheritance - not yet needed
        final var custom = new AnnotationFinder(new ClassesArchive(mapper)).findAnnotatedMethods(Mapping.Custom.class);
        return "=== Mapper " + mapper.getSimpleName() + "\n" +
                "\n" +
                conf.documentation().strip() + '\n' +
                "\n" +
                "==== Types:\n" +
                "\n" +
                "* From: `" + conf.from() + "`\n" +
                "* To: `" + conf.to() + "`\n" +
                "\n" +
                Stream.of(conf.tables())
                        .filter(it -> it.entries().length > 0)
                        .map(t -> "" +
                                "===== " + t.name() + "\n" +
                                "\n" +
                                tablePrefix.replace("{{name}}", t.name()) +
                                "[.table-" + t.name() + ",options=\"header\",cols=\"a,\"]\n" +
                                "|===\n" +
                                "|Input|Output\n" +
                                Stream.of(t.entries())
                                        .map(it -> "| " + it.input() + "|" + it.output())
                                        .collect(joining("\n")) + '\n' +
                                "|===")
                        .collect(joining("\n\n", "==== Lookup Tables\n\n", "\n\n")) +
                "==== Mappings:\n" +
                "\n" +
                Stream.concat(
                        Stream.of(conf.properties())
                                .map(it -> switch (it.type()) {
                                    case MAPPED -> it.to() + ":: read from the incoming `" + it.from() + "` property";
                                    case CONSTANT -> it.to() + ":: set to `" + it.value() + "` value";
                                    case TABLE_MAPPING -> it.to() + ":: looked up in `" + it.value() + "` table lookup. When missing, " + switch (it.onMissedTableLookup()) {
                                        case FAIL -> "a failure is issued.";
                                        case NULL -> "`null` is used.";
                                        case FORWARD -> "value is kept as that.";
                                    };
                                }),
                        custom.stream()
                                .map(m -> {
                                    final var doc = m.getAnnotation(Mapping.Custom.class);
                                    final var target = doc.to();
                                    return (target.isEmpty() ? m.getName() : target) + ":: " + doc.description();
                                }))
                        .sorted()
                        .collect(joining("\n", "", "\n\n"));
    }
}
