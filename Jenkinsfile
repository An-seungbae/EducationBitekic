pipeline {
    agent any

    environment {
        MENDIX_DIR = 'C:\\Program Files\\Mendix\\10.24.0.73019\\modeler'
        MPR_FILE = 'EducationBitekic.mpr'
        MDA_FILE = 'EducationBitekic.mda'
        WAR_FILE = 'EducationBitekic.war' // WAR 파일 이름 (Mendix Studio Pro에서 내보낸 이름과 일치해야 함)
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.7.6-hotspot' // Mendix 버전과 호환되는 JDK 경로
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
                // .mda 파일을 빌드합니다.
                bat """
                    "${env.MENDIX_DIR}\\mxbuild.exe" "${env.MPR_FILE}" --java-home "${env.JAVA_HOME}" --java-exe-path "${env.JAVA_HOME}\\bin\\java.exe" --target=package --output "${env.MDA_FILE}"
                """
                echo "Build completed: ${env.MDA_FILE} created."
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo "Archiving build artifacts..."
                archiveArtifacts artifacts: "*.mda, *.war", followSymlinks: false
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
