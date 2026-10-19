plugins {
    id("st.evening.mc.prelude.build") version "1.0.0"
}

group = "st.evening.mc.exalembico"
version = "1.0.2"

preludeBuild {
    modId = "exalembico"
    mod {
        constantsClass = "ExAlembicoConsts"
    }
    minecraft {
        mcVersion = "1.12.2"
        deobf {
            mappingsChannel = "stable"
            mappingsVersion = "39"
        }
    }
}

repositories {
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "CleanroomMC"
        url = uri("https://repo.cleanroommc.com/releases")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev")
    }
}

dependencies {
    api("mezz:jei:4.29.15")
    implementation("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.1.20.711")
    implementation(rfg.deobf("curse.maven:zen-foundry-303857:3200702")) // 1.7.18
}
