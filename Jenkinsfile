// Jenkinsfile
pipeline {
    // Mendix Studio Pro가 설치된 Windows Agent를 사용하도록 레이블 지정
    agent any // 어떤 Agent에서든 실행되도록 변경

    // 환경 변수 설정
    environment {
        // Mendix 설치 경로 (Agent 환경에 맞게 수정)
        MENDIX_DIR = 'C:\\Program Files\\Mendix\\10.24.0.73019\\modeler' 
        // Mendix 프로젝트 파일 이름 (.mpr 파일)
        MPR_FILE = 'EducationBitekic.mpr'
        // 생성될 빌드 패키지 파일 이름
        MDA_FILE = 'EducationBitekic.mda'
		JAVA_HOME = 'C:\\Java\\jdk-17'    
		}

    stages {
        // 1. Git에서 소스 코드 가져오기
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        // 2. Mendix 프로젝트 빌드
        stage('Build Mendix App') {
            steps {
                echo "Starting Mendix build for ${env.MPR_FILE}..."
                // Windows Agent에서 실행되므로 'bat' 사용
                bat """
                    "${env.MENDIX_DIR}\\mxbuild.exe" --target=package --output="${env.MDA_FILE}" "${env.MPR_FILE}" "${env.JAVA_HOME}"
                """
                echo "Build completed: ${env.MDA_FILE} created."
            }
        }

        // 3. 빌드 결과물(MDA 파일) 저장
        stage('Archive Artifacts') {
            steps {
                echo "Archiving build artifacts..."
                archiveArtifacts artifacts: "*.mda", followSymlinks: false
            }
        }
    }

    // 4. 빌드 후 정리 작업
    post {
        always {
            echo 'Build process finished. Cleaning up workspace...'
            cleanWs() // Workspace 정리
        }
    }
}