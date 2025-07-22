pipeline {
    agent any

    environment {
        MENDIX_DIR = 'C:\\Program Files\\Mendix\\10.24.0.73019\\modeler'
        MPR_FILE = 'EducationBitekic.mpr'
        MDA_FILE = 'EducationBitekic.mda' // Mendix 빌드 아티팩트 (MDA) 파일
        JAVA_HOME = 'C:\\Java\\jdk-17'
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
                // Windows 환경에서 파일 경로에 공백이 있을 수 있으므로 따옴표로 감싸는 것이 안전합니다.
                bat 'if exist "${MPR_FILE}.lock" del "${MPR_FILE}.lock"'
            }
        }
        stage('Build Mendix App') {
            steps {
                echo "Starting Mendix build for ${env.MPR_FILE}..."
                bat """
                    "${env.MENDIX_DIR}\\mxbuild.exe" "${env.MPR_FILE}" ^
                    --java-home "${env.JAVA_HOME}" ^
					--java-exe-path "${env.JAVA_HOME}\\bin\\java.exe" ^ // 이 줄이 추가되었습니다.
                    --target=package ^
                    --output "${env.MDA_FILE}"
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