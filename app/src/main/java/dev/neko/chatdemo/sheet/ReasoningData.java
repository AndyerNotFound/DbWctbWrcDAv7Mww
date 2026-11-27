package dev.neko.chatdemo.sheet;

import java.util.ArrayList;
import java.util.List;

/** Sample provider / model data shared by the two reasoning sheet pages. */
final class ReasoningData {

    static final class Provider {
        final String name;
        final List<Group> groups = new ArrayList<>();

        Provider(String name) {
            this.name = name;
        }
    }

    static final class Group {
        final String name;
        final Model[] models;

        Group(String name, Model[] models) {
            this.name = name;
            this.models = models;
        }
    }

    static final class Model {
        final String name;
        final String meta;
        final String desc;
        final String avatar;
        final int style;

        Model(String name, String meta, String desc, String avatar, int style) {
            this.name = name;
            this.meta = meta;
            this.desc = desc;
            this.avatar = avatar;
            this.style = style;
        }
    }

    static List<Provider> build() {
        List<Provider> providers = new ArrayList<>();

        Provider openAi = new Provider("OpenAI");
        openAi.groups.add(new Group("Flagship", new Model[]{
                new Model("GPT-5.4", "OpenAI · 200k context · vision",
                        "Flagship reasoning, tools and vision.", "G", 0),
                new Model("GPT-5.4 Pro", "OpenAI · 400k context · vision",
                        "Slow, expensive, very good at hard plans.", "P", 0)
        }));
        openAi.groups.add(new Group("Fast", new Model[]{
                new Model("GPT-5.4 mini", "OpenAI · 128k context",
                        "Cheap and quick for small refactors.", "m", 2)
        }));

        Provider anthropic = new Provider("Anthropic");
        anthropic.groups.add(new Group("Flagship", new Model[]{
                new Model("Claude Opus 4.5", "Anthropic · 200k context",
                        "Long reviews and careful edits.", "C", 1)
        }));
        anthropic.groups.add(new Group("Balanced", new Model[]{
                new Model("Claude Sonnet 4.5", "Anthropic · 200k context",
                        "The everyday pairing model.", "S", 1)
        }));

        Provider google = new Provider("Google");
        google.groups.add(new Group("Fast", new Model[]{
                new Model("Gemini 3 Flash", "Google · 1M context",
                        "Huge documents, fast first token.", "G", 2)
        }));

        Provider local = new Provider("Local");
        local.groups.add(new Group("On device", new Model[]{
                new Model("Neko-8B", "Local · 32k context",
                        "Private, runs on the phone.", "N", 3)
        }));

        providers.add(openAi);
        providers.add(anthropic);
        providers.add(google);
        providers.add(local);
        return providers;
    }

    private ReasoningData() {
    }
}
