const fs = require('fs');
const path = require('path');

module.exports = function(context) {
    const gradleConfigPath = path.join(context.opts.projectRoot, 'platforms/android/cdv-gradle-config.json');
    const appBuildGradlePath = path.join(context.opts.projectRoot, 'platforms/android/app/build.gradle');
    
    // Atualizar cdv-gradle-config.json
    fs.readFile(gradleConfigPath, 'utf8', (err, data) => {
        if (err) {
            console.error('-- ❌ Erro ao ler cdv-gradle-config.json:', err);
            throw err;
        }

        let config = JSON.parse(data);

        if (config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED === false) {
            config.IS_GRADLE_PLUGIN_GOOGLE_SERVICES_ENABLED = true;

            fs.writeFile(gradleConfigPath, JSON.stringify(config, null, 2), (err) => {
                if (err) {
                    console.error('-- ❌ Erro ao atualizar cdv-gradle-config.json:', err);
                    throw err;
                } else {
                    console.log('-- ✅ cdv-gradle-config.json atualizado com sucesso para habilitar o Google Services.');
                }
            });
        } else {
            console.log('-- ✅ Google Services já está configurado como TRUE em cdv-gradle-config.json.');
        }
    });

    // Verificar e ajustar build.gradle
    fs.readFile(appBuildGradlePath, 'utf8', (err, data) => {
        if (err) {
            console.error('-- ❌ Erro ao ler app/build.gradle:', err);
            throw err;
        }

        const googleServicesPlugin = "apply plugin: 'com.google.gms.google-services'";
        const occurrences = (data.match(new RegExp(googleServicesPlugin, 'g')) || []).length;

        if (occurrences > 1) {
            console.log('-- ⚠️ Encontrado múltiplas ocorrências de com.google.gms.google-services. Ajustando para uma única ocorrência.');

            // Remover todas as ocorrências
            let updatedData = data.replace(new RegExp(googleServicesPlugin, 'g'), '');

            // Adicionar uma única ocorrência no final do arquivo
            updatedData += `\n${googleServicesPlugin}\n`;

            fs.writeFile(appBuildGradlePath, updatedData, 'utf8', (err) => {
                if (err) {
                    console.error('-- ❌ Erro ao escrever app/build.gradle:', err);
                    throw err;
                } else {
                    console.log('-- ✅ app/build.gradle ajustado para uma única ocorrência de com.google.gms.google-services.');
                }
            });
        } else {
            console.log('-- ✅ Nenhuma duplicata encontrada no app/build.gradle.');
        }
    });
};
