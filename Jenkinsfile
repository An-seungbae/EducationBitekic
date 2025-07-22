pipeline {
    agent any

    environment {
        MENDIX_DIR = 'C:\\Program Files\\Mendix\\10.24.0.73019\\modeler'
        MPR_FILE = 'EducationBitekic.mpr'
        MDA_FILE = 'EducationBitekic.mda'
        WAR_FILE = 'EducationBitekic.war' // WAR 파일 이름 (Mendix Studio Pro에서 내보낸 이름과 일치해야 함)
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.7.6-hotspot' // Mendix 버전과 호환되는 JDK 경로
        TOMCAT_WEBAPPS_DIR = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps' // Tomcat webapps 경로
        TOMCAT_BIN_DIR = 'C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\bin' // Tomcat bin 경로
        // Mendix 런타임 설정 파일 경로 (Tomcat 서버에 미리 생성되어 있어야 함)
        MENDIX_SETTINGS_PATH = 'C:\\MendixConfig\\EducationBitekic\\settings.yaml'
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
        stage('Package as WAR') {
            steps {
                bat """
                mkdir war\\WEB-INF\\classes
                xcopy /E /I /Y build\\model war\\WEB-INF\\classes
                jar -cvf EducationBitekic.war -C war .
                """
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                bat 'copy "EducationBitekic.war" "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps\\"'
            }
        }
        // 이 단계는 Mendix Studio Pro에서 수동으로 WAR 파일을 생성했거나,
        // 별도의 스크립트/도구로 WAR 파일이 생성되어 Jenkins 작업 공간에 존재한다고 가정합니다.
        // 만약 Studio Pro에서 WAR 파일을 내보내는 것을 Jenkins에서 직접 자동화하고 싶다면,
        // 해당 기능을 제공하는 Mendix API나 툴이 필요합니다. (mxbuild.exe는 직접 WAR를 만들지 않음)
        stage('Copy WAR to Tomcat') {
            steps {
                echo "Copying ${env.WAR_FILE} to Tomcat webapps directory..."
                // 기존 WAR 파일 및 압축 해제된 디렉토리 삭제 (클린 배포를 위해)
                bat "if exist \"${env.TOMCAT_WEBAPPS_DIR}\\%WAR_FILE%\" del \"${env.TOMCAT_WEBAPPS_DIR}\\%WAR_FILE%\""
                bat "if exist \"${env.TOMCAT_WEBAPPS_DIR}\\%WAR_FILE%.substring(0, %WAR_FILE%.lastIndexOf('.'))%\" rmdir /s /q \"${env.TOMCAT_WEBAPPS_DIR}\\%WAR_FILE%.substring(0, %WAR_FILE%.lastIndexOf('.'))%\""

                // WAR 파일 복사
                bat "copy \"${env.WAR_FILE}\" \"${env.TOMCAT_WEBAPPS_DIR}\""
                echo "WAR file copied to Tomcat."
            }
        }

        stage('Restart Tomcat') {
            steps {
                echo "Restarting Tomcat server..."
                // Tomcat 서비스 중지 (서비스 이름은 환경에 따라 다를 수 있음)
                // net stop "Tomcat9" // 또는 서비스 이름 확인 후 사용
                // net start "Tomcat9"

                // 또는 Tomcat bin 디렉토리의 shutdown.bat/startup.bat 사용
                bat "cd \"${env.TOMCAT_BIN_DIR}\" && call shutdown.bat"
                // Tomcat이 완전히 종료될 때까지 잠시 대기
                sleep 10 // 10초 대기 (필요에 따라 조절)

                // Mendix 런타임 설정을 위한 환경 변수 설정 (setenv.bat에 설정되어 있지 않다면)
                // 이 방법은 임시적인 설정이며, 영구적인 설정은 Tomcat setenv.bat에 추가하는 것이 좋습니다.
                bat "set M2EE_RUNTIME_SETTINGS=\"${env.MENDIX_SETTINGS_PATH}\" && cd \"${env.TOMCAT_BIN_DIR}\" && call startup.bat"
                echo "Tomcat restarted."
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
