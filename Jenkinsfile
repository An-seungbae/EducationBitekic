pipeline {
    agent any

    environment {
        MENDIX_DIR = 'C:\\Program Files\\Mendix\\10.24.0.73019\\modeler'
        MPR_FILE = 'EducationBitekic.mpr'
        MDA_FILE = 'EducationBitekic.mda' // Mendix 빌드 아티팩트 (MDA) 파일
        JAVA_HOME = 'C:\\Java\\jdk-21.0.7.6-hotspot'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }
        stage('Clean Lock') {
            steps {
                echo "Deleting .mpr.lock file if it exists..."
                bat 'if exist "${MPR_FILE}.lock" del "${MPR_FILE}.lock"'
            }
        }
        stage('Build Mendix App') {
            steps {
                echo "Starting Mendix build for ${env.MPR_FILE}..."
                // mxbuild 명령어를 한 줄로 연결하고, 불필요한 주석을 제거했습니다.
                bat """
                    "${env.MENDIX_DIR}\\mxbuild.exe" "${env.MPR_FILE}" --java-home "${env.JAVA_HOME}" --java-exe-path "${env.JAVA_HOME}\\bin\\java.exe" --target=package --output "${env.MDA_FILE}"
                """
                echo "Build completed: ${env.MDA_FILE} created."
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo "Archiving build artifacts..."
                archiveArtifacts artifacts: "*.mda", followSymlinks: false
            }
        }
    }

    post {
        always {
            echo 'Build process finished. Cleaning up workspace...'
            cleanWs()
        }
    }
}