plugins {
  id("org.ajoberstar.grgit") version "4.0.0"
  id("base")
}
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.BranchListOp

var grgit:Grgit? = null
val githubRootURL  = "https://github.com/prb28"
val rootRepoDir  = "${project.buildDir}/git-repos"

tasks.addRule("Pattern: cloneRepo<ID>") {
    val taskName = this
    if (startsWith("cloneRepo")) {
        task(taskName) {
            val paramString = taskName.replace("cloneRepo", "")
            val params = paramString.split("__")
            val branchName = params[0]
            val repoName = params[1]
            val repoURL = "${githubRootURL}/${repoName}"
            val repoDir = file("${rootRepoDir}/${repoName}/${branchName}")
            outputs.dir(repoDir).withPropertyName("outputDir")
            doLast {
                if (repoDir.list().size > 0) {
                    println("Opening $repoDir")
                    grgit = Grgit.open{dir = repoDir}
                    grgit!!.fetch()
                } else {
                    println("Cloning $repoDir")
                    grgit = Grgit.clone{dir = repoDir; uri = repoURL}
                }
            }
        }
    }
}


tasks.addRule("Pattern: checkoutBinaries<ID>") {
    val taskName = this
    if (startsWith("checkoutBinaries")) {
        task(taskName) {
            val paramString = taskName.replace("checkoutBinaries", "")
            val params = paramString.split("__")
            val branchName = params[0].toLowerCase()
            val repoName = params[1]
            val cloneTask = tasks["cloneRepo${paramString}"]
            dependsOn(cloneTask)
            outputs.dirs(cloneTask.getOutputs().getFiles()).withPropertyName("outputDir")
            doLast {
                val remoteBranch = grgit!!.resolve.toBranch("origin/${branchName}")
                val branches = grgit!!.branch.list{mode = BranchListOp.Mode.LOCAL}
                val slectedBranches = branches.filter { it.getName().contains(branchName) }
                if (slectedBranches.size > 0) {
                    grgit!!.checkout{branch = branchName}
                } else {
                    grgit!!.checkout{branch = branchName; createBranch= true; startPoint=remoteBranch}
                }
            }
        }
    }
}

tasks.addRule("Pattern: zipBinaries<ID>") {
    val taskName = this
    if (startsWith("zipBinaries")) {
        task<Zip>(taskName) {
            val paramString = taskName.replace("zipBinaries", "")
            val params = paramString.split("__")
            val branchName = params[0]
            val repoName = params[1]
            val repoDir = file("${rootRepoDir}/${repoName}")
            archiveFileName.set("${branchName.toLowerCase()}.zip")
            from(tasks["checkoutBinaries${paramString}"])
            include("**/*")
        }
    }
}

tasks.addRule("Pattern: zipRepo<ID>") {
    val taskName = this
    if (startsWith("zipRepo")) {
        task<Zip>(taskName) {
            val paramString = taskName.replace("zipRepo", "")
            val params = paramString.split("__")
            val binaryType = params[0]
            val repoName = params[1]
            from (tasks["cloneRepomaster__${repoName}"])
            from (tasks["checkoutBinaries${binaryType}__vscode-amiga-assembly-binaries"]) {
                into("bin")
            }
            exclude(".git*")
            archiveFileName.set("${repoName.toLowerCase()}-${binaryType.toLowerCase()}.zip")
            include("**/*")
        }
    }
}


task("zipBinaries") {
    dependsOn("zipBinariesOsx__vscode-amiga-assembly-binaries", "zipRepoOsx__vscode-amiga-wks-example", "zipRepoOsx__vscode-amiga-vbcc-example", 
        "zipBinariesWindows_x64__vscode-amiga-assembly-binaries", "zipRepoWindows_x64__vscode-amiga-wks-example", "zipRepoWindows_x64__vscode-amiga-vbcc-example", 
        "zipBinariesDebian_x64__vscode-amiga-assembly-binaries", "zipRepoDebian_x64__vscode-amiga-wks-example", "zipRepoDebian_x64__vscode-amiga-vbcc-example")
}