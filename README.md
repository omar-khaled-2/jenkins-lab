# CI/CD Lab - Jenkins Learning Path (Zero to Hero)

This project is a minimal monorepo for learning Jenkins CI/CD concepts from beginner to advanced levels.

## Project Structure

```
ci-cd-lab/
├── package.json          # Root workspace config
├── README.md
├── backend/              # Node.js API
│   ├── package.json
│   ├── server.js
│   └── server.test.js
└── frontend/             # React app
    ├── package.json
    ├── index.html
    ├── vite.config.js
    └── src/
        ├── main.jsx
        └── App.jsx
```

## Prerequisites

- Docker & Docker Compose
- Git
- Basic Linux commands
- Node.js (local testing)

---

## Phase 1: Jenkins Basics (Beginner)

### 1.1 Install Jenkins via Docker

```bash
# Create a Docker Compose file for Jenkins
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  jenkins:
    image: jenkins/jenkins:lts
    ports:
      - "8080:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - JAVA_OPTS=-Djenkins.install.runSetupWizard=false

volumes:
  jenkins_home:
EOF

# Start Jenkins
docker-compose up -d

# Get initial admin password (if setup wizard enabled)
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Access Jenkins at: `http://localhost:8080`

### 1.2 Your First Job (Freestyle)

**Exercise:** Create a freestyle job that:
1. Pulls this repository from Git
2. Runs `npm install`
3. Runs `npm test`

**Steps:**
1. New Item → "ci-cd-lab-basic" → Freestyle project
2. Source Code Management → Git → `file:///path/to/your/repo`
3. Build Steps → Execute shell:
   ```bash
   npm install
   npm test
   ```
4. Save → Build Now

### 1.3 Jenkinsfile Introduction

Create a `Jenkinsfile` in the repo root:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Install') {
            steps {
                sh 'npm install'
            }
        }
        
        stage('Test') {
            steps {
                sh 'npm test'
            }
        }
    }
}
```

**Exercise:** Create a Pipeline job pointing to your Jenkinsfile.

---

## Phase 2: Core Concepts (Intermediate)

### 2.1 Agents & Labels

**Concept:** Agents are execution environments.

```groovy
pipeline {
    agent { label 'docker-agent' }
    
    stages {
        stage('Build') {
            agent { docker 'node:20-alpine' }
            steps {
                sh 'node --version'
                sh 'npm install'
            }
        }
    }
}
```

**Exercise:** Configure a Docker agent and run the build inside a Node.js container.

### 2.2 Environment Variables

```groovy
pipeline {
    agent any
    
    environment {
        NODE_ENV = 'production'
        API_URL = 'http://localhost:3001'
    }
    
    stages {
        stage('Build') {
            steps {
                echo "Building for ${env.NODE_ENV}"
                sh 'npm run build'
            }
        }
    }
}
```

### 2.3 Credentials Management

**Exercise:** Store sensitive data securely.

1. Jenkins Dashboard → Manage Jenkins → Credentials
2. Add Secret Text: `NPM_TOKEN`
3. Use in pipeline:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Deploy') {
            steps {
                withCredentials([string(credentialsId: 'NPM_TOKEN', variable: 'TOKEN')]) {
                    sh 'echo "Token is available: $TOKEN"'
                }
            }
        }
    }
}
```

### 2.4 Parallel Execution

```groovy
pipeline {
    agent any
    
    stages {
        stage('Test') {
            parallel {
                stage('Backend Tests') {
                    steps {
                        dir('backend') {
                            sh 'npm install'
                            sh 'npm test'
                        }
                    }
                }
                stage('Frontend Tests') {
                    steps {
                        dir('frontend') {
                            sh 'npm install'
                            sh 'npm test'
                        }
                    }
                }
            }
        }
    }
}
```

**Exercise:** Run backend and frontend tests in parallel.

### 2.5 Post-build Actions

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'npm run build'
            }
        }
    }
    
    post {
        always {
            echo 'This always runs'
            cleanWs()
        }
        success {
            echo 'Build succeeded!'
        }
        failure {
            echo 'Build failed!'
            mail to: 'team@example.com',
                 subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
                 body: "Something is wrong with ${env.BUILD_URL}"
        }
    }
}
```

---

## Phase 3: Advanced Pipelines

### 3.1 Multi-Branch Pipeline

**Concept:** Automatically builds all branches and PRs.

**Exercise:** 
1. Create a Multi-branch Pipeline job
2. Point to your Git repository
3. Jenkins will discover branches automatically
4. Create a feature branch and see it build

### 3.2 Shared Libraries

Create reusable pipeline code:

**Directory structure:**
```
jenkins/
└── vars/
    └── standardBuild.groovy
```

**`vars/standardBuild.groovy`:**
```groovy
def call(Map config) {
    pipeline {
        agent any
        stages {
            stage('Build') {
                steps {
                    sh "${config.buildCommand}"
                }
            }
            stage('Test') {
                steps {
                    sh "${config.testCommand}"
                }
            }
        }
    }
}
```

**Usage in Jenkinsfile:**
```groovy
@Library('my-shared-library') _

standardBuild(
    buildCommand: 'npm run build',
    testCommand: 'npm test'
)
```

### 3.3 Docker Integration

```groovy
pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "myapp:${BUILD_NUMBER}"
    }
    
    stages {
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}")
                }
            }
        }
        
        stage('Run Tests in Container') {
            steps {
                script {
                    docker.image("${DOCKER_IMAGE}").inside {
                        sh 'npm test'
                    }
                }
            }
        }
        
        stage('Push to Registry') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-credentials') {
                        docker.image("${DOCKER_IMAGE}").push()
                    }
                }
            }
        }
    }
}
```

**Exercise:** Build and push both backend and frontend Docker images.

### 3.4 Complete Monorepo Pipeline

```groovy
pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }
    
    stages {
        stage('Detect Changes') {
            steps {
                script {
                    def changes = currentBuild.rawBuild.changeSets
                    env.BACKEND_CHANGED = changes.any { it.affectedFiles.any { it.path.startsWith('backend/') } }
                    env.FRONTEND_CHANGED = changes.any { it.affectedFiles.any { it.path.startsWith('frontend/') } }
                }
            }
        }
        
        stage('Install Dependencies') {
            steps {
                sh 'npm install'
            }
        }
        
        stage('Test') {
            parallel {
                stage('Backend') {
                    when { expression { env.BACKEND_CHANGED == 'true' } }
                    steps {
                        dir('backend') {
                            sh 'npm test'
                        }
                    }
                }
                stage('Frontend') {
                    when { expression { env.FRONTEND_CHANGED == 'true' } }
                    steps {
                        dir('frontend') {
                            sh 'npm test'
                        }
                    }
                }
            }
        }
        
        stage('Build') {
            when { branch 'main' }
            steps {
                dir('frontend') {
                    sh 'npm run build'
                }
                archiveArtifacts artifacts: 'frontend/dist/**', fingerprint: true
            }
        }
        
        stage('Deploy') {
            when { branch 'main' }
            steps {
                echo 'Deploying application...'
                // Add your deployment logic here
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
```

---

## Phase 4: Jenkins Administration (Hero Level)

### 4.1 Master-Agent Architecture

**Concept:** Distribute builds across multiple machines.

**Exercise:**
1. Set up Jenkins master
2. Configure agents (nodes) for different purposes:
   - `linux-agent`: For Node.js builds
   - `docker-agent`: For Docker builds
   - `windows-agent`: For Windows builds

### 4.2 Security

- Enable security realm (LDAP/Active Directory)
- Configure matrix-based security
- Use role-based access control (RBAC)
- Enable CSRF protection
- Use build tokens for webhooks

### 4.3 Monitoring & Maintenance

```groovy
// Disk space monitoring pipeline
pipeline {
    agent any
    
    stages {
        stage('Cleanup') {
            steps {
                sh 'docker system prune -f'
                cleanWs()
            }
        }
        
        stage('Health Check') {
            steps {
                sh 'df -h'
                sh 'docker ps'
            }
        }
    }
    
    triggers {
        cron('H 2 * * *') // Run daily at 2 AM
    }
}
```

### 4.4 Pipeline as Code Best Practices

1. **Keep pipelines DRY** - Use shared libraries
2. **Version your Jenkinsfiles** - Store in repo
3. **Use declarative syntax** - Easier to read
4. **Fail fast** - Validate early
5. **Small, focused stages** - Better visibility
6. **Use timeouts** - Prevent hung builds
7. **Archive artifacts** - Preserve build outputs

### 4.5 Blue-Green Deployment Example

```groovy
pipeline {
    agent any
    
    environment {
        BLUE_ENV = 'app-blue'
        GREEN_ENV = 'app-green'
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'npm run build'
            }
        }
        
        stage('Deploy to Green') {
            steps {
                script {
                    // Deploy to green environment
                    sh "docker-compose up -d ${GREEN_ENV}"
                    // Health check
                    sh 'sleep 10'
                    sh 'curl -f http://green-env:3000/health'
                }
            }
        }
        
        stage('Switch Traffic') {
            steps {
                script {
                    // Switch load balancer to green
                    sh './switch-traffic.sh green'
                    // Stop blue environment
                    sh "docker-compose stop ${BLUE_ENV}"
                }
            }
        }
    }
}
```

---

## Learning Exercises Checklist

### Beginner
- [x] Install Jenkins locally via Docker
- [x] Create a freestyle job that builds this project
- [x] Convert to a Pipeline job with a Jenkinsfile
- [x] Add environment variables to the pipeline

### Intermediate
- [x] Run backend and frontend tests in parallel
- [x] Use Docker agents for builds
- [x] Implement post-build notifications (email/Slack)
- [x] Store and use credentials securely
- [ ] Set up a multi-branch pipeline

### Advanced
- [ ] Create a shared library for common steps
- [ ] Implement conditional builds based on changed files
- [ ] Build Docker images for both services
- [ ] Push images to a container registry
- [ ] Implement blue-green deployment
- [ ] Set up master-agent architecture

### Hero
- [ ] Configure RBAC and security policies
- [ ] Implement GitOps workflow with Jenkins
- [ ] Set up monitoring and alerting for Jenkins
- [ ] Create self-service pipeline templates
- [ ] Implement canary deployments
- [ ] Integrate with Kubernetes for deployments

---

## Useful Commands

```bash
# Check Jenkins logs
docker logs jenkins

# Restart Jenkins
docker restart jenkins

# Backup Jenkins home
tar -czvf jenkins-backup.tar.gz /var/jenkins_home

# Install Jenkins CLI
wget http://localhost:8080/jnlpJars/jenkins-cli.jar

# Run CLI commands
java -jar jenkins-cli.jar -s http://localhost:8080/ who-am-i
```

## Resources

- [Jenkins Handbook](https://www.jenkins.io/doc/book/)
- [Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Shared Libraries](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
- [Jenkins Docker Images](https://hub.docker.com/r/jenkins/jenkins)

## Next Steps

1. Complete exercises in order
2. Experiment with the provided pipeline examples
3. Add real deployment targets (AWS, Azure, on-prem)
4. Integrate with testing frameworks (Jest, Cypress, etc.)
5. Add code quality gates (SonarQube, ESLint)
6. Implement feature flags for safer deployments

Happy learning! 🚀
